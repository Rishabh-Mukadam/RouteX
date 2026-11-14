package com.ridam.routex.management.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.internal.util.StringHelper;

@AllArgsConstructor
@Getter
public class LoginResponse {
    private String token ;
    private String tokenType;
    private long expiresInSeconds;
    private String role;
    private String OrganizationId;
}
