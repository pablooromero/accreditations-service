package com.accreditations_service.accreditations_service.services;

import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

public interface SalePointClientService {
    @CircuitBreaker(name = "salesPointBreaker", fallbackMethod = "getSalePointNameFallback")
    @Retry(name = "salesPointRetry", fallbackMethod = "getSalePointNameFallback")
    @RateLimiter(name = "salesPointRateLimiter", fallbackMethod = "getSalePointNameFallback")
    String getSalePointName(Long salePointId) throws SalePointException;
}
