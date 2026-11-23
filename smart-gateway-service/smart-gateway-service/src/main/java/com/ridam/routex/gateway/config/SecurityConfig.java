package com.ridam.routex.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity){
        httpSecurity
                .csrf(csrf->csrf.disable())
                .authorizeHttpRequests(auth->{
//            auth.requestMatchers("/health").permitAll()
                    auth.anyRequest().permitAll();

        });
        return httpSecurity.build();

    }
}
