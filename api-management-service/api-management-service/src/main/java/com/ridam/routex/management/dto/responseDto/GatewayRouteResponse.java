package com.ridam.routex.management.dto.responseDto;

import com.ridam.routex.management.entity.ApiRoute;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.RouteMatcher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class GatewayRouteResponse extends RouteResponse {
    private String slug;

}
