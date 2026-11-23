# RouteX --- Custom API Gateway with Spring Boot

RouteX is a backend-focused API Gateway and API Management platform
built with Spring Boot. It provides a controlled entry point between API
clients and backend services, applies configured technical entry
policies, and forwards permitted requests to their target services.

The project demonstrates the internal responsibilities of an API gateway
and the separation of live request processing, configuration management,
and analytics. It is an educational implementation with a defined scope,
not a replacement for a mature production gateway.

> **Important architectural boundary:** RouteX handles gateway-level
> technical policies. Business rules, resource ownership checks, domain
> validation, and business-specific authorization remain inside the
> target backend services. RouteX must not absorb or replace those
> business responsibilities.

------------------------------------------------------------------------

## Contents

-   [Why RouteX](#why-routex)
-   [Core responsibilities](#core-responsibilities)
-   [Architecture overview](#architecture-overview)
-   [Services and ownership](#services-and-ownership)
-   [Request and configuration flows](#request-and-configuration-flows)
-   [Technology and data ownership](#technology-and-data-ownership)
-   [Planned gateway capabilities](#planned-gateway-capabilities)
-   [Performance and latency
    approach](#performance-and-latency-approach)
-   [Failure behavior](#failure-behavior)
-   [Project scope and exclusions](#project-scope-and-exclusions)
-   [Initial setup milestone](#initial-setup-milestone)
-   [Design principles](#design-principles)

------------------------------------------------------------------------

## Why RouteX

When an application is split into multiple backend services, clients
should not need to know the location and routing details of every
service. Backend services may also otherwise repeat technical concerns
such as:

-   Authentication-token validation
-   Broad access and role checks
-   Request-rate enforcement
-   Request/correlation ID handling
-   Access-event recording
-   Consistent technical error handling

RouteX provides one controlled entry point. A client sends a request to
the gateway; RouteX evaluates the applicable route configuration and
technical policies, forwards an allowed request to the configured
target, and returns the target response.

### The three main gateway jobs

  -----------------------------------------------------------------------
Responsibility                      Meaning
  ----------------------------------- -----------------------------------
Routing                             Select the target service using the
incoming request and route
configuration.

Policy enforcement                  Apply configured authentication,
role, rate-limit, idempotency, and
other entry-point rules.

Reverse proxying                    Send the HTTP request to the target
service and return its response.
  -----------------------------------------------------------------------

Routing selects the destination; proxying performs the HTTP exchange.
RouteX acts as a reverse proxy on behalf of backend services.

## Core responsibilities

RouteX separates technical gateway responsibilities from domain/business
responsibilities.

**RouteX is responsible for:** - Deciding which configured target
receives a request. - Applying configured gateway-level authentication
and role requirements. - Applying configured rate limits and POST
idempotency handling. - Propagating or generating correlation IDs. -
Applying supported path and header rules. - Forwarding requests over
HTTP with route-specific timeouts. - Producing consistent gateway
errors. - Publishing access and configuration events for asynchronous
processing.

**Target services remain responsible for:** - Business/domain
validation. - Resource ownership checks. - Business-specific
authorization. - Business operations and transaction rules.

For example, RouteX may verify that a caller has an administrator role
before allowing a protected route. The target service must still decide
whether that administrator is allowed to modify the particular resource.

------------------------------------------------------------------------

## Architecture overview

RouteX is organized around three main applications and small downstream
demo services.

``` text
                         +----------------------+
                         |       API Client     |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | Smart Gateway Service|
                         |      DATA PLANE      |
                         +----+------------+----+
                              |            |
                       HTTP   |            | Redis
                              v            v
                    +----------------+  +------------------+
                    | Catalog / Order|  | Policy state     |
                    | demo services  |  | rate limits and  |
                    +----------------+  | idempotency      |
                                        +------------------+

       Admin / Postman
              |
              v
   +-------------------------+       +----------------------+
   | API Management Service  |------>|        MySQL         |
   |       CONTROL PLANE     |       | users, roles, routes,|
   +------------+------------+       | versions, audit data |
                |                    +----------------------+
                |
                | Kafka configuration-change notification
                v
        +----------------------+
        |        Kafka         |
        +----------+-----------+
                   |
                   | access events
                   v
        +----------------------+
        |  Analytics Service   |
        +----------+-----------+
                   |
                   v
        +----------------------+
        |       MongoDB        |
        | access and processing|
        | records              |
        +----------------------+
```

The architecture has three distinct concerns:

  -----------------------------------------------------------------------
Plane                   Service                 Responsibility
  ----------------------- ----------------------- -----------------------
Data plane              Smart Gateway Service   Apply already-loaded
rules to live requests
and proxy permitted
traffic.

Control plane           API Management Service  Define, store, version,
and change users,
routes, and gateway
policies.

Analytics               Analytics Service       Consume access events,
deduplicate them, and
store operational
analytics data.
  -----------------------------------------------------------------------

### Data plane and control plane

The Smart Gateway Service is the data plane. It needs a short and
predictable request path.

The API Management Service is the control plane. It handles
administration and durable configuration storage.

The Gateway loads active route configuration into memory. During a
temporary Management Service outage, already-loaded routes can continue
to be used. New configuration loads and updates require the Management
Service to become available again.

------------------------------------------------------------------------

## Services and ownership

### 1. Smart Gateway Service

**Role:** Public entry point and data plane.

Responsibilities: - Match incoming requests against active routes held
in memory. - Generate or propagate correlation IDs. - Validate JWTs
locally using configured key material. - Enforce route-level role
requirements. - Apply Redis-backed rate limiting. - Apply configured
POST idempotency. - Rewrite request paths and add/remove selected
headers. - Forward requests to target services over HTTP. - Enforce
route-specific response timeouts. - Return consistent gateway errors. -
Publish access-event metadata to Kafka.

The Gateway: - Holds the active route registry in memory. - Loads and
refreshes route configuration through the API Management Service. - Uses
Redis for temporary policy state. - Does not directly access the
Management Service's MySQL tables. - Does not own MySQL or MongoDB
data. - Does not own business-domain tables.

### 2. API Management Service

**Role:** Control plane for users, routes, and policies.

Responsibilities: - Manage user credentials and roles. - Authenticate
management users and issue signed JWTs. - Create, store, update, and
manage route configuration. - Maintain configuration versions. - Publish
route-change notifications to Kafka. - Record administrative actions
using focused AOP auditing.

**Data ownership:** This service owns the MySQL data for users, roles,
routes, policy settings, configuration versions, and audit records.
Other services must not directly modify its tables.

### 3. Analytics Service

**Role:** Asynchronous processing of Gateway access events.

Responsibilities: - Consume access events from Kafka. - Deduplicate
events using a unique `eventId`. - Store access-event documents and
processing records in MongoDB. - Provide operational statistics such as
request counts, outcomes, average duration, slow routes, rate-limit
rejections, and idempotency replays.

**Data ownership:** This service owns MongoDB data for access events and
processed-event records.

The Analytics Service does not administer routes and does not
participate in forwarding live requests.

### 4. Catalog and Order demo services

These are small downstream target services used to demonstrate: -
Request routing and forwarding. - Gateway-level authorization. - Path
rewriting. - Header propagation. - Route-specific timeouts. - Duplicate
request handling.

Their business logic is intentionally small. They remain responsible for
their own business-level validation and authorization.

------------------------------------------------------------------------

## Request and configuration flows

### Live request flow

1.  An API client sends an HTTP request to the Smart Gateway Service.
2.  The Gateway identifies the route using its in-memory route registry.
3.  The Gateway applies the configured local security and role checks.
4.  If the route requires shared policy state, the Gateway consults
    Redis for rate limiting or idempotency.
5.  The Gateway applies configured path/header rules and forwards the
    request to the target service over HTTP.
6.  The target service performs its own business validation and business
    operation.
7.  The target response returns through the Gateway to the client.
8.  The Gateway publishes access-event metadata to Kafka for
    asynchronous analytics processing.

The Management Service, MySQL, Analytics Service, and MongoDB are not
contacted for every forwarded request.

### Configuration update flow

1.  An administrator changes route or policy configuration through the
    API Management Service.
2.  The Management Service persists the change in MySQL.
3.  The Management Service publishes a route-change notification to the
    `gateway-configuration-events` Kafka topic.
4.  The Gateway receives the notification.
5.  The Gateway fetches the latest configuration from the Management
    Service.
6.  The Gateway refreshes its in-memory route registry.

Kafka carries the notification; MySQL remains the durable source of
truth through the Management Service. This flow occurs during startup
and configuration refresh, not for every client request.

### Analytics flow

1.  The Gateway publishes request outcome metadata to the
    `gateway-access-events` Kafka topic.
2.  The Analytics Service consumes the events independently.
3.  The Analytics Service uses `eventId` to avoid storing the same event
    more than once.
4.  The Analytics Service stores the event and processing information in
    MongoDB.

The client does not wait for analytics processing to complete.
Asynchronous publishing still has a cost, and producer failures require
explicit handling; asynchronous delivery alone does not guarantee that
events can never be lost.

### Event fields and sensitive data

Useful access-event metadata includes: - `eventId` - `correlationId` -
Route - Request method - Outcome - Request duration - Whether a
rate-limit rejection occurred - Whether an idempotency replay occurred

**Never include passwords, JWTs, cookies, or authorization headers in
access events.**

`correlationId` links work associated with one incoming request.
`eventId` identifies an individual event and supports deduplication.
They serve different purposes.

------------------------------------------------------------------------

## Technology and data ownership

  -----------------------------------------------------------------------
Technology              Responsibility          Data / usage
  ----------------------- ----------------------- -----------------------
Java and Spring Boot    Application framework   Independent backend
services

HTTP                    Direct service          Gateway-to-target
communication           forwarding and
configuration retrieval

MySQL                   Durable configuration   Users, roles, routes,
database                policies, versions, and
audit records; owned by
API Management

Redis                   Immediate shared        Rate-limit counters and
runtime state           temporary idempotency
state

Kafka                   Asynchronous event      Access events and
infrastructure          configuration-change
notifications

MongoDB                 Analytics persistence   Access-event documents
and processing records;
owned by Analytics
  -----------------------------------------------------------------------

### Redis and Kafka are not interchangeable

  -----------------------------------------------------------------------
Concern                 Redis                   Kafka
  ----------------------- ----------------------- -----------------------
Main purpose            Immediate shared state  Asynchronous event
delivery

Typical RouteX data     Counters and            Access and
idempotency state       configuration events

Gateway usage           Obtain a result needed  Notify another
for a policy decision   component about an
outcome or change

Retention               Usually short-lived and Controlled by topic
controlled by TTL       retention
  -----------------------------------------------------------------------

### Redis usage

Redis is used for short-lived shared state needed while deciding whether
a request can continue.

-   **Rate limiting:** Redis stores counters. The Gateway updates
    counters atomically, checks the configured limit, and rejects
    requests that exceed it. TTL removes counters after their useful
    window.
-   **POST idempotency:** For configured routes, Redis stores temporary
    processing state and a saved successful response associated with an
    idempotency key. A matching completed request can receive the saved
    response without forwarding the operation again.
-   **Atomicity:** Related counter and expiry operations can be grouped
    into one atomic operation, for example with a Redis Lua script, to
    avoid race conditions caused by separate reads and writes.

Redis is not the authoritative store for route configuration or
analytics history. It is not used as a general backend-response cache.

### Route configuration and in-memory registry

MySQL is the durable source of route configuration, accessed through the
API Management Service. The Gateway keeps a local in-memory copy for
fast request matching.

Routes are loaded at startup and refreshed when configuration changes.
The Gateway does not query MySQL directly for each request.

A route can describe: - Destination target. - Allowed HTTP methods. -
Authentication requirements. - Permitted roles. - Request limit. -
Response timeout. - Active status. - Supported path and header
transformation rules. - Whether idempotency is enabled.

Dynamic routing means route definitions are configuration, rather than
hard-coded conditional logic. Updating a route does not require changing
and rebuilding the Gateway source code.

### Local JWT validation

The Management Service authenticates users and issues signed JWTs. The
Gateway validates those tokens locally rather than making an
authentication-service call for every protected request.

This avoids a recurring network call. The Gateway does not fetch current
user information on every request. Token expiry, key rotation, and
revocation are separate concerns; full token revocation is outside the
defined project scope.

------------------------------------------------------------------------

## Planned gateway capabilities

The architecture describes the following capabilities. They are planned
system responsibilities and should not automatically be interpreted as
already implemented.

  -----------------------------------------------------------------------
Area                                Capability
  ----------------------------------- -----------------------------------
Request identification              Generate or propagate a correlation
ID.

Routing                             Match active routes using
configuration held in memory.

Security                            Validate JWTs locally and enforce
route-level role requirements.

Traffic control                     Use Redis for rate limiting.

Duplicate request handling          Use Redis for configured POST
idempotency.

Forwarding                          Proxy HTTP requests with
route-specific timeouts.

Transformation                      Rewrite paths and add or remove
selected headers.

Error handling                      Return consistent gateway errors.

Analytics                           Publish access events to Kafka for
later processing.

Configuration                       Store and version route settings;
notify the Gateway about changes.

Auditing                            Record administrative actions using
focused AOP auditing.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## Performance and latency approach

An API Gateway introduces an additional network hop and processing work.
RouteX cannot provide zero additional latency. Its design aims to keep
the added work limited by avoiding unnecessary synchronous dependencies.

  -----------------------------------------------------------------------
Work                    Where it happens        Reason
  ----------------------- ----------------------- -----------------------
Route matching          Gateway memory          Avoid a Management
Service or database
lookup for every
request.

JWT validation          Gateway process         Validate using
configured key material
without a recurring
network call.

Role checks             Gateway process         Compare authenticated
roles with the loaded
route policy.

Rate limiting           Redis                   Maintain shared
counters with atomic
operations.

Idempotency             Redis                   Coordinate duplicate
requests using shared
temporary state.

Path/header changes     Gateway process         Apply loaded
transformation rules
locally.

Analytics storage       Analytics Service       Process events outside
the live response path.
  -----------------------------------------------------------------------

The Gateway does not synchronously call separate authentication,
routing, logging, and analytics services for every request. Redis is
consulted only when a configured policy requires shared state. The
target service is then contacted over HTTP.

------------------------------------------------------------------------

## Failure behavior

The separation of services reduces unnecessary dependencies, but it does
not make the system immune to failures.

  -----------------------------------------------------------------------
Unavailable component               Intended effect
  ----------------------------------- -----------------------------------
API Management Service              Already-loaded routes remain
usable; administration and
configuration refresh pause.

Analytics Service                   Request forwarding continues;
retained Kafka events can be
processed later.

MongoDB                             Request forwarding continues;
analytics storage is affected.

Target service                      Requests to that target fail
through the Gateway.

Slow target service                 The configured route timeout limits
waiting.

Redis                               Routes requiring Redis-backed
policy decisions fail closed in
this project.

Kafka                               Event publication or delivery is
affected; producer failures need
explicit handling.
  -----------------------------------------------------------------------

**Fail closed** means refusing a request when a required policy cannot
be checked, rather than silently bypassing that policy.

A Gateway with already-loaded routes can continue during a temporary
Management Service outage. However, new configuration loads and updates
depend on Management becoming available again.

------------------------------------------------------------------------

## Project scope and exclusions

RouteX focuses on a defined set of gateway and API-management
responsibilities.

The following are outside the scope described by the architecture:

-   Spring Cloud Gateway, Eureka, and Spring Cloud Config.
-   Service discovery and load balancing.
-   Circuit breakers and automatic retries.
-   General response caching.
-   Request or response body transformation.
-   XML-to-JSON conversion.
-   WebSocket, gRPC, and GraphQL proxying.
-   File uploads and streaming proxy support.
-   TLS termination.
-   API keys, billing, and quota products.
-   Business-process orchestration.

These capabilities may be useful in other systems, but are excluded to
keep RouteX focused. Automatic retries are specifically excluded because
they can add delay and may duplicate writes.

------------------------------------------------------------------------

## Initial setup milestone

The initial implementation milestone is to create and start three
independent Spring Boot applications:

``` text
smart-gateway-service
api-management-service
analytics-service
```

Each application should: 1. Start independently. 2. Use a distinct port.
3. Respond to a basic HTTP health-check request.

A health check confirms that an application can start and respond over
HTTP. It does not by itself prove that Redis, Kafka, MySQL, MongoDB, or
the complete gateway workflow is ready.

The architecture notes do not specify concrete port numbers,
health-check endpoint paths, controller implementations, or exact run
commands. Those values should be documented from the actual application
configuration rather than guessed.

------------------------------------------------------------------------

## Design principles

1.  **Keep business logic in target services.** RouteX enforces
    gateway-level technical policies; target services own domain
    behavior and resource-level decisions.
2.  **Separate data plane and control plane.** Live request processing
    should not depend on management operations for every request.
3.  **Keep route matching local.** Use an in-memory registry refreshed
    from the Management Service.
4.  **Keep data ownership clear.** API Management owns MySQL
    configuration data; Analytics owns MongoDB analytics data.
5.  **Use Redis for shared temporary policy state.** Rate limiting and
    idempotency need shared state across Gateway instances.
6.  **Use Kafka for asynchronous events.** Access analytics and
    configuration-change notifications should not become synchronous
    work in the live request path.
7.  **Avoid unnecessary synchronous dependencies.** Keep the request
    path predictable and reduce avoidable latency.
8.  **Fail closed when a required policy cannot be evaluated.** Do not
    silently bypass security or traffic-control decisions.
9.  **Treat planned features honestly.** The architecture describes the
    target design; implementation status must be verified against the
    codebase.

------------------------------------------------------------------------

## Quick revision

  -----------------------------------------------------------------------
Concept                             Remember
  ----------------------------------- -----------------------------------
API Gateway                         One controlled entry point for
routing, policies, and proxying.

Data plane                          Smart Gateway handles live
requests.

Control plane                       API Management defines and stores
the rules.

In-memory routes                    Avoid repeated configuration
lookups during forwarding.

Local JWT validation                Avoid an authentication-service
call for every token.

Redis                               Shared temporary state for rate
limiting and idempotency.

Kafka                               Access events and
configuration-change notifications.

MySQL                               Durable configuration owned by API
Management.

MongoDB                             Analytics data owned by Analytics.

Business logic                      Remains in target services.

Current milestone                   Independent application startup and
a basic health check.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## Architecture source

This README is based on the supplied **GateKeeper-Architecture-Notes**
(15 pages). The project name has been updated to **RouteX** while
preserving the architecture, responsibilities, data ownership,
boundaries, planned capabilities, and initial milestone described in
those notes.

Where the source describes a capability as planned, this README retains
that distinction rather than claiming the capability is already
implemented.
