package com.accreditations_service.accreditations_service.interceptors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;

@Slf4j
public class JwtForwardingInterceptor implements ClientHttpRequestInterceptor {

    private static final String AUTH_HEADER = HttpHeaders.AUTHORIZATION;
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String tokenValue = null;

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof Jwt) {
                tokenValue = ((Jwt) principal).getTokenValue();
                log.debug("JwtForwardingInterceptor: Token JWT extraído del Principal (Jwt object).");
            } else if (authentication.getCredentials() instanceof String) {
                String credentials = (String) authentication.getCredentials();
                if (isPotentialJwt(credentials)) {
                    tokenValue = credentials;
                    log.debug("JwtForwardingInterceptor: Token extraído de Credentials.");
                }
            }
            if (tokenValue != null) {
                if (!request.getHeaders().containsKey(AUTH_HEADER)) {
                    request.getHeaders().add(AUTH_HEADER, BEARER_PREFIX + tokenValue);
                    log.info("JwtForwardingInterceptor: Header 'Authorization' añadido a la petición saliente a URI: {}", request.getURI());
                } else {
                    log.warn("JwtForwardingInterceptor: Header 'Authorization' ya presente en la petición saliente a URI: {}. No se sobrescribirá.", request.getURI());
                }
            } else {
                log.warn("JwtForwardingInterceptor: No se pudo extraer el token JWT del SecurityContext actual para la petición a URI: {}", request.getURI());
            }
        } else {
            log.warn("JwtForwardingInterceptor: No hay autenticación en el SecurityContextHolder para la petición a URI: {}. No se añadirá token.", request.getURI());
        }
        return execution.execute(request, body);
    }

    private boolean isPotentialJwt(String token) {
        return token != null && token.split("\\.").length == 3;
    }
}
