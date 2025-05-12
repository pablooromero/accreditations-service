package com.accreditations_service.accreditations_service.config;

import com.accreditations_service.accreditations_service.interceptors.JwtForwardingInterceptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        JwtForwardingInterceptor jwtForwardingInterceptor = new JwtForwardingInterceptor();

        return builder
                .additionalInterceptors(jwtForwardingInterceptor)
                .build();
    }
}
