package com.ridam.routex.management.service;

import com.nimbusds.jose.crypto.impl.MACProvider;
import com.ridam.routex.management.dto.requestDto.LoginRequest;
import com.ridam.routex.management.dto.responseDto.LoginResponse;
import com.ridam.routex.management.entity.ManagementUser;
import com.ridam.routex.management.entity.Organization;
import com.ridam.routex.management.entity.OrganizationStatus;
import com.ridam.routex.management.exception.ApiException;
import com.ridam.routex.management.repository.ManagementUserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthenticationService {
    private ManagementUserRepository managementUserRepository;
    private PasswordEncoder passwordEncoder;
    private JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public AuthenticationService(
            ManagementUserRepository managementUserRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.expiration-seconds}")long expirationSeconds

    ){
        this.managementUserRepository=managementUserRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtEncoder=jwtEncoder;
        this.expirationSeconds = expirationSeconds;
    }
    @Transactional(readOnly = true)
    public LoginResponse login(@Valid LoginRequest request) {
        ManagementUser user=managementUserRepository.findByUsername(request.getUsername())
                .orElseThrow(()->invalidCredentials());

        if(user.isEnabled()==false
                || passwordEncoder.matches(request.getPassword(),user.getPassword_hash())==false
        ){
            throw invalidCredentials();
        }

        if(user.getOrganization()!=null && user.getOrganization().getStatus()!= OrganizationStatus.ACTIVE){
            throw new ApiException(HttpStatus.FORBIDDEN,"Organization is not active");
        }

        Instant issuedAt=Instant.now();
        String role=user.getRole().getName();
        Long organizationId= user.getOrganization()==null?null:user.getOrganization().getId();


        JwtClaimsSet.Builder claims=JwtClaimsSet.builder()
                .issuer("routex-api-management")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(expirationSeconds))
                .subject(user.getUsername())
                .claim("role",role);

        if (organizationId!=null){
            claims.claim("organizationId",organizationId);
        }

        JwsHeader header=JwsHeader.with(MacAlgorithm.HS256).build();
        String token =jwtEncoder.encode(
                JwtEncoderParameters.from(header,claims.build())
        ).getTokenValue();

        return new LoginResponse(
                token,
                "Bearer",
                expirationSeconds,
                role,
                organizationId==null?null:Long.toString(organizationId)
        );

    }

    private RuntimeException invalidCredentials(){
        return new ApiException(HttpStatus.UNAUTHORIZED,"Invalid username or password");
    }
}
