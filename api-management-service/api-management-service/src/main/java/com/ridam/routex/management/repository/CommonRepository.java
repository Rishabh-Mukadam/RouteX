package com.ridam.routex.management.repository;

import com.ridam.routex.management.dto.responseDto.GatewayRouteResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CommonRepository {

    private JdbcTemplate template;

    public List<GatewayRouteResponse> findAllGatewayRoutes(){
        String sql= """
                SELECT r.*, o.slug AS organization_slug
                            FROM api_routes r
                            JOIN organizations o ON r.organization_id = o.id
                            WHERE r.active = true
                            and o.status ="ACTIVE"
                
                """;

        List<GatewayRouteResponse> res=template.query(sql,new BeanPropertyRowMapper<>(GatewayRouteResponse.class));

        return res;
    }
}
