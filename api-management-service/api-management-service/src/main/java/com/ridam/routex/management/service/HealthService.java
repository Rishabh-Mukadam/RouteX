package com.ridam.routex.management.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HealthService {

    private  JdbcTemplate jdbcTemplate;

    public String getDBHealthStatus(){
        try{
            Integer res=jdbcTemplate.queryForObject(
                    "select 1", Integer.class
            );
            if(res==1){
                return "UP";
            }
        }
        catch (Exception e){
            System.out.println(e);
            return "Exception: "+e;
        }
        return "Down";
    }
}
