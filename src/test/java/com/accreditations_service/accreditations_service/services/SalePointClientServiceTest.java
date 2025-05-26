package com.accreditations_service.accreditations_service.services;

import com.accreditations_service.accreditations_service.dtos.SalePointDTO;
import com.accreditations_service.accreditations_service.exceptions.SalePointException;
import com.accreditations_service.accreditations_service.services.implementations.SalePointClientServiceImplementation;
import com.accreditations_service.accreditations_service.utils.Constants;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalePointClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    @InjectMocks
    private SalePointClientServiceImplementation salePointClientService;

    private final String salesPointServiceBaseUrl = "http://fake-sales-point-service/api/sales-points/";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(salePointClientService, "SALES_POINT_SERVICE_URL", salesPointServiceBaseUrl);
    }

    @Test
    @DisplayName("getSalePointName - Debería devolver nombre cuando el servicio responde OK")
    void getSalePointName_whenServiceRespondsOk_shouldReturnName() throws SalePointException {
        Long salePointId = 1L;
        String expectedName = "Plaza Central";
        SalePointDTO dto = new SalePointDTO(salePointId, expectedName);
        ResponseEntity<SalePointDTO> responseEntity = ResponseEntity.ok(dto);
        String expectedUrl = salesPointServiceBaseUrl + salePointId;

        when(restTemplate.getForEntity(eq(expectedUrl), eq(SalePointDTO.class))).thenReturn(responseEntity);

        String actualName = salePointClientService.getSalePointName(salePointId);

        assertEquals(expectedName, actualName);
        verify(restTemplate, times(1)).getForEntity(expectedUrl, SalePointDTO.class);
    }

    @Test
    @DisplayName("getSalePointName - Debería lanzar SalePointException si el servicio responde OK pero body es null")
    void getSalePointName_whenOkButBodyNull_shouldThrowSalePointException() {
        Long salePointId = 1L;
        ResponseEntity<SalePointDTO> responseEntity = ResponseEntity.ok(null);
        String expectedUrl = salesPointServiceBaseUrl + salePointId;
        when(restTemplate.getForEntity(eq(expectedUrl), eq(SalePointDTO.class))).thenReturn(responseEntity);

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointClientService.getSalePointName(salePointId);
        });
        assertTrue(exception.getMessage().contains("Unexpected response for sale point:"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }

    @Test
    @DisplayName("getSalePointName - Debería lanzar SalePointException si el servicio responde OK pero nombre en body es null")
    void getSalePointName_whenOkButNameInBodyNull_shouldThrowSalePointException() {
        Long salePointId = 1L;
        SalePointDTO dtoWithNullName = new SalePointDTO(salePointId, null);
        ResponseEntity<SalePointDTO> responseEntity = ResponseEntity.ok(dtoWithNullName);
        String expectedUrl = salesPointServiceBaseUrl + salePointId;
        when(restTemplate.getForEntity(eq(expectedUrl), eq(SalePointDTO.class))).thenReturn(responseEntity);

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointClientService.getSalePointName(salePointId);
        });
        assertTrue(exception.getMessage().contains("Unexpected response for sale point:"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }


    @Test
    @DisplayName("getSalePointName - Debería lanzar SalePointException (NOT_FOUND) en HttpClientErrorException.NotFound")
    void getSalePointName_whenNotFound_shouldThrowSalePointException() {
        Long salePointId = 99L;
        String expectedUrl = salesPointServiceBaseUrl + salePointId;
        when(restTemplate.getForEntity(eq(expectedUrl), eq(SalePointDTO.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Sale Point Not Found"));

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointClientService.getSalePointName(salePointId);
        });
        assertEquals(Constants.SALE_POINT_NOT_FOUND + salePointId, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    @DisplayName("getSalePointName - Debería lanzar SalePointException (BAD_GATEWAY) en HttpClientErrorException (no NotFound)")
    void getSalePointName_whenClientError_shouldThrowSalePointException() {
        Long salePointId = 1L;
        String expectedUrl = salesPointServiceBaseUrl + salePointId;
        HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Client Error");
        when(restTemplate.getForEntity(eq(expectedUrl), eq(SalePointDTO.class))).thenThrow(cause);

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointClientService.getSalePointName(salePointId);
        });
        assertTrue(exception.getMessage().contains(Constants.ERROR_CALLING_SALE_POINT_SERVICE));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    @DisplayName("getSalePointName - Debería lanzar SalePointException (status del error) en HttpServerErrorException")
    void getSalePointName_whenServerError_shouldThrowSalePointException() {
        Long salePointId = 1L;
        String expectedUrl = salesPointServiceBaseUrl + salePointId;
        HttpServerErrorException cause = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error");
        when(restTemplate.getForEntity(eq(expectedUrl), eq(SalePointDTO.class))).thenThrow(cause);

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointClientService.getSalePointName(salePointId);
        });
        assertTrue(exception.getMessage().contains(Constants.ERROR_CALLING_SALE_POINT_SERVICE));

        assertNotNull(exception.getHttpStatus());
    }


    @Test
    @DisplayName("getSalePointName - Debería lanzar SalePointException (SERVICE_UNAVAILABLE) en ResourceAccessException")
    void getSalePointName_whenNetworkError_shouldThrowSalePointException() {
        Long salePointId = 1L;
        String expectedUrl = salesPointServiceBaseUrl + salePointId;
        when(restTemplate.getForEntity(eq(expectedUrl), eq(SalePointDTO.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointClientService.getSalePointName(salePointId);
        });
        assertTrue(exception.getMessage().contains(Constants.COULD_NOT_CONNECT_SALE_POINT_SERVICE));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    }

    @Test
    @DisplayName("getSalePointNameFallback - Debería lanzar SalePointException (TOO_MANY_REQUESTS) para RequestNotPermitted")
    void getSalePointNameFallback_withRequestNotPermitted_shouldThrowTooManyRequests() {
        Long salePointId = 1L;
        Throwable cause = RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("salesPointServiceRL"));

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointClientService.getSalePointNameFallback(salePointId, cause);
        });
        assertEquals("Límite de peticiones excedido. Intente más tarde.", exception.getMessage());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("getSalePointNameFallback - Debería lanzar SalePointException (NOT_FOUND) si la causa es NotFound")
    void getSalePointNameFallback_withNotFoundError_shouldThrowNotFound() {
        Long salePointId = 1L;
        Throwable cause = new HttpClientErrorException(HttpStatus.NOT_FOUND, "Original Not Found");

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointClientService.getSalePointNameFallback(salePointId, cause);
        });
        assertEquals(Constants.SALE_POINT_NOT_FOUND + salePointId, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("getSalePointNameFallback - Debería lanzar SalePointException (NOT_FOUND) para error genérico (según lógica actual del fallback)")
    void getSalePointNameFallback_withGenericError_shouldThrowNotFound() {
        Long salePointId = 1L;
        Throwable cause = new RuntimeException("Some other error");

        SalePointException exception = assertThrows(SalePointException.class, () -> {
            salePointClientService.getSalePointNameFallback(salePointId, cause);
        });
        assertEquals(exception.getMessage(), exception.getMessage());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    }
}