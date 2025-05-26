package com.accreditations_service.accreditations_service.services;

import com.accreditations_service.accreditations_service.exceptions.UserException;
import com.accreditations_service.accreditations_service.services.implementations.UserClientServiceImplementation;
import com.accreditations_service.accreditations_service.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    @InjectMocks
    private UserClientServiceImplementation userClientService;

    private final String userServiceBaseUrl = "http://fake-user-service/api/users/";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userClientService, "USER_SERVICE_URL", userServiceBaseUrl);
    }

    @Test
    @DisplayName("getUserIdFromEmail - Debería devolver ID de usuario cuando el servicio responde OK")
    void getUserIdFromEmail_whenServiceRespondsOk_shouldReturnUserId() throws UserException {
        String email = "test@example.com";
        Long expectedUserId = 123L;
        String expectedUrl = userServiceBaseUrl + "private/email/" + email;
        when(restTemplate.getForObject(eq(expectedUrl), eq(Long.class))).thenReturn(expectedUserId);

        Long actualUserId = userClientService.getUserIdFromEmail(email);

        assertEquals(expectedUserId, actualUserId);
        verify(restTemplate, times(1)).getForObject(expectedUrl, Long.class);
    }

    @Test
    @DisplayName("getUserIdFromEmail - Debería lanzar UserException si el servicio devuelve null")
    void getUserIdFromEmail_whenServiceReturnsNull_shouldThrowUserException() {
        String email = "test@example.com";
        String expectedUrl = userServiceBaseUrl + "private/email/" + email;
        when(restTemplate.getForObject(eq(expectedUrl), eq(Long.class))).thenReturn(null);

        UserException exception = assertThrows(UserException.class, () -> {
            userClientService.getUserIdFromEmail(email);
        });
        assertEquals(Constants.USER_ID_NULL_RESPONSE + email, exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }

    @Test
    @DisplayName("getUserIdFromEmail - Debería lanzar UserException (NOT_FOUND) en HttpClientErrorException.NotFound")
    void getUserIdFromEmail_whenNotFound_shouldThrowUserException() {
        String email = "notfound@example.com";
        String expectedUrl = userServiceBaseUrl + "private/email/" + email;
        when(restTemplate.getForObject(eq(expectedUrl), eq(Long.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "User Not Found"));

        UserException exception = assertThrows(UserException.class, () -> {
            userClientService.getUserIdFromEmail(email);
        });
        assertEquals(Constants.USER_NOT_FOUND + email, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    @DisplayName("getUserIdFromEmail - Debería lanzar UserException (SERVICE_UNAVAILABLE) en ResourceAccessException")
    void getUserIdFromEmail_whenNetworkError_shouldThrowUserException() {
        String email = "networkerror@example.com";
        String expectedUrl = userServiceBaseUrl + "private/email/" + email;
        when(restTemplate.getForObject(eq(expectedUrl), eq(Long.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        UserException exception = assertThrows(UserException.class, () -> {
            userClientService.getUserIdFromEmail(email);
        });
        assertTrue(exception.getMessage().contains(Constants.COULD_NOT_CONNECT_USER_SERVICE));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    }

    @Test
    @DisplayName("getUserIdFromEmail - Debería lanzar UserException (status del error) en HttpServerErrorException")
    void getUserIdFromEmail_whenServerError_shouldThrowUserException() {
        String email = "servererror@example.com";
        String expectedUrl = userServiceBaseUrl + "private/email/" + email;
        when(restTemplate.getForObject(eq(expectedUrl), eq(Long.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        UserException exception = assertThrows(UserException.class, () -> {
            userClientService.getUserIdFromEmail(email);
        });
        assertTrue(exception.getMessage().contains(Constants.ERROR_CALLING_USER_SERVICE));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }


    @Test
    @DisplayName("getUserIdFromEmailFallback - Debería lanzar UserException (SERVICE_UNAVAILABLE) para error genérico")
    void getUserIdFromEmailFallback_withGenericError_shouldThrowServiceUnavailable() {
        String email = "fallback@example.com";
        Throwable genericCause = new RuntimeException("Generic failure");

        UserException exception = assertThrows(UserException.class, () -> {
            userClientService.getUserIdFromEmailFallback(email, genericCause);
        });
        assertTrue(exception.getMessage().contains(Constants.FALLBACK_USER_ID_FROM_EMAIL_ERROR + email));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
        assertEquals(genericCause, exception.getCause());
    }

    @Test
    @DisplayName("getUserIdFromEmailFallback - Debería lanzar UserException (NOT_FOUND) si la causa es NotFound")
    void getUserIdFromEmailFallback_withNotFoundError_shouldThrowNotFound() {
        String email = "fallback.notfound@example.com";
        Throwable notFoundCause = new HttpClientErrorException(HttpStatus.NOT_FOUND, "Original Not Found");

        UserException exception = assertThrows(UserException.class, () -> {
            userClientService.getUserIdFromEmailFallback(email, notFoundCause);
        });
        assertTrue(exception.getMessage().contains(Constants.USER_NOT_FOUND + email));
        assertTrue(exception.getMessage().contains("Fallback"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertEquals(notFoundCause, exception.getCause());
    }

    @Test
    @DisplayName("getUserIdFromEmailFallback - Debería lanzar UserException (NOT_FOUND) si la causa es UserException con NOT_FOUND")
    void getUserIdFromEmailFallback_withUserExceptionNotFound_shouldThrowNotFound() {
        String email = "fallback.userexception.notfound@example.com";
        Throwable userExceptionNotFound = new UserException("User not found originally", HttpStatus.NOT_FOUND);

        UserException exception = assertThrows(UserException.class, () -> {
            userClientService.getUserIdFromEmailFallback(email, userExceptionNotFound);
        });
        assertTrue(exception.getMessage().contains(Constants.USER_NOT_FOUND + email));
        assertTrue(exception.getMessage().contains("Fallback"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertEquals(userExceptionNotFound, exception.getCause());
    }
}
