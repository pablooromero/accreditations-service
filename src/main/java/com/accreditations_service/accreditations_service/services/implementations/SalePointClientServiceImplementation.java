package com.accreditations_service.accreditations_service.services.implementations;

import com.accreditations_service.accreditations_service.dtos.SalePointDTO;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.services.SalePointClientService;
import com.accreditations_service.accreditations_service.utils.Constants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalePointClientServiceImplementation implements SalePointClientService {

    private final RestTemplate restTemplate;

    @Value("${SALES_POINT_SERVICE_URL}")
    private String SALES_POINT_SERVICE_URL;

    private Integer attempt = 0;

    @CircuitBreaker(name = "salesPointServiceCB", fallbackMethod = "getSalePointNameFallback")
    @Retry(name = "salesPointServiceRetry", fallbackMethod = "getSalePointNameFallback")
    @RateLimiter(name = "salesPointServiceRL", fallbackMethod = "getSalePointNameFallback")
    @Override
    public String getSalePointName(Long salePointId) throws SalePointException {
        log.info(Constants.GET_SALE_POINT_NAME, salePointId);
        String salePointServiceUrl = SALES_POINT_SERVICE_URL + salePointId;

        try {
            ResponseEntity<SalePointDTO> response = restTemplate.getForEntity(salePointServiceUrl, SalePointDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && response.getBody().name() != null) {
                log.info(Constants.GET_SALE_POINT_NAME_SUCCESSFULLY + response.getBody().name());
                return response.getBody().name();
            } else {
                log.error("Unexpected error for ID: {}. Status: {}, Body: {}",
                        salePointId, response.getStatusCode(), response.getBody());
                throw new SalePointException("Unexpected response for sale point: " + salePointId, HttpStatus.NOT_FOUND);
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn(Constants.SALE_POINT_NOT_FOUND + "{}", salePointId, e);
            throw new SalePointException(Constants.SALE_POINT_NOT_FOUND + salePointId, HttpStatus.NOT_FOUND);
        } catch (HttpClientErrorException e) {
            log.warn(Constants.ERROR_CALLING_SALE_POINT_SERVICE + " - Client Error for ID: {}. Status: {}",
                    salePointId, e.getStatusCode(), e);
            throw new SalePointException(Constants.ERROR_CALLING_SALE_POINT_SERVICE + e.getMessage(), HttpStatus.BAD_GATEWAY);
        } catch (HttpServerErrorException e) {
            log.error(Constants.ERROR_CALLING_SALE_POINT_SERVICE + " - Server Error for ID: {}. Status: {}",
                    salePointId, e.getStatusCode(), e);
            throw new SalePointException(Constants.ERROR_CALLING_SALE_POINT_SERVICE + e.getMessage());
        } catch (ResourceAccessException e) {
            log.error(Constants.COULD_NOT_CONNECT_SALE_POINT_SERVICE + " for ID: {}", salePointId, e);
            throw new SalePointException(Constants.COULD_NOT_CONNECT_SALE_POINT_SERVICE, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public String getSalePointNameFallback(Long salePointId, Throwable t) throws SalePointException {
        log.warn("[FALLBACK] Fallback para getSalePointName ejecutado para salePointId: {}. Causa: {} - {}",
                salePointId, t.getClass().getSimpleName(), t.getMessage());
        attempt++;

        if (t instanceof RequestNotPermitted) {
            log.warn("[FALLBACK] Rate limit excedido para getSalePointName con ID: {}", salePointId);
            throw new SalePointException("Límite de peticiones excedido. Intente más tarde.", HttpStatus.TOO_MANY_REQUESTS, t);
        } else if (t instanceof HttpClientErrorException.NotFound) {
            log.warn("[FALLBACK] El punto de venta con ID: {} no fue encontrado (desde fallback).", salePointId);
            throw new SalePointException(Constants.SALE_POINT_NOT_FOUND + salePointId, HttpStatus.NOT_FOUND, t);
        }
        log.error("Fallback triggered for getSalePointName: {}", t.getMessage());
        throw new SalePointException(Constants.SALE_POINT_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

}
