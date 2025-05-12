package com.accreditations_service.accreditations_service.services.implementations;

import com.accreditations_service.accreditations_service.exceptions.UserException;
import com.accreditations_service.accreditations_service.services.UserClientService;
import com.accreditations_service.accreditations_service.utils.Constants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserClientServiceImplementation implements UserClientService {

    private final RestTemplate restTemplate;

    @Value("${USER_SERVICE_URL}")
    private String USER_SERVICE_URL;

    @Override
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "getUserIdFromEmailFallback")
    @Retry(name = "userServiceRetry", fallbackMethod = "getUserIdFromEmailFallback")
    public Long getUserIdFromEmail(String email) throws UserException {
        log.info(Constants.GET_USER_ID_FROM_EMAIL + " (Resilience4j Protected Call) for email: {}", email);
        String url = USER_SERVICE_URL + "private/email/" + email;

        try {
            Long userId = restTemplate.getForObject(url, Long.class);

            // Es importante verificar si la respuesta es null, ya que getForObject puede devolver null
            // si el cuerpo de la respuesta es vacío o no se puede convertir.
            if (userId == null) {
                log.error(Constants.USER_ID_NULL_RESPONSE + " for email: {}", email);
                // Esta UserException, al tener un HttpStatus.INTERNAL_SERVER_ERROR (o similar),
                // debería ser registrada por el Circuit Breaker si UserException está en recordExceptions.
                throw new UserException(Constants.USER_ID_NULL_RESPONSE + email, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            log.info(Constants.GET_USER_ID_SUCCESSFULLY + " for email: {}. User ID: {}", email, userId);
            return userId;

        } catch (HttpClientErrorException.NotFound e) {
            // Este error (404) usualmente no debería abrir el Circuit Breaker.
            // La UserException resultante (con HttpStatus.NOT_FOUND)
            // debería ser ignorada por el CB si HttpClientErrorException está en ignoreExceptions.
            log.warn(Constants.USER_NOT_FOUND + email, e);
            throw new UserException(Constants.USER_NOT_FOUND + email, HttpStatus.NOT_FOUND, e);
        } catch (HttpClientErrorException e) { // Otros errores 4xx
            log.warn(Constants.ERROR_CALLING_USER_SERVICE + " - Client Error for email: {}. Status: {}",
                    email, e.getStatusCode(), e);
            throw new UserException(Constants.ERROR_CALLING_USER_SERVICE + e.getMessage(), (HttpStatus) e.getStatusCode(), e);
        } catch (HttpServerErrorException e) { // Errores 5xx del user-service
            // Estos SÍ deberían contar para el Circuit Breaker y Retries.
            log.error(Constants.ERROR_CALLING_USER_SERVICE + " - Server Error for email: {}. Status: {}",
                    email, e.getStatusCode(), e);
            // La UserException resultante debería ser registrada por el CB.
            throw new UserException(Constants.ERROR_CALLING_USER_SERVICE + e.getMessage(), (HttpStatus) e.getStatusCode(), e);
        } catch (ResourceAccessException e) { // Errores de red, timeouts de conexión
            // Estos SÍ deberían contar para el Circuit Breaker y Retries.
            log.error(Constants.COULD_NOT_CONNECT_USER_SERVICE + " for email: {}", email, e);
            // La UserException resultante debería ser registrada por el CB.
            throw new UserException(Constants.COULD_NOT_CONNECT_USER_SERVICE + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, e);
        }
    }

    public Long getUserIdFromEmailFallback(String email, Throwable t) throws UserException {
        log.warn("[FALLBACK] Fallback para getUserIdFromEmail ejecutado para email: {}. Causa: {} - {}",
                email, t.getClass().getSimpleName(), t.getMessage());

        // Si la causa original ya era una UserException (por ejemplo, un 404 que no queremos que se convierta en SERVICE_UNAVAILABLE)
        // podríamos querer relanzarla tal cual, especialmente si el CB la ignoró.
        // Sin embargo, si el fallback se activa porque el CB está abierto o los reintentos por fallos de red/servidor se agotaron,
        // entonces una excepción de servicio no disponible es más apropiada.
        if (t instanceof HttpClientErrorException.NotFound ||
                (t instanceof UserException ue && ue.getHttpStatus() == HttpStatus.NOT_FOUND)) {
            log.warn("[FALLBACK] Usuario no encontrado para email: {} (desde fallback).", email);
            throw new UserException(Constants.USER_NOT_FOUND + email + " (Fallback: servicio contactado, usuario no existe)", HttpStatus.NOT_FOUND, t);
        }

        // Para otras causas (CircuitBreaker abierto, Retries agotados por errores de servidor/red)
        // Obtener el ID de usuario es crítico para la creación de la acreditación.
        // Lanzar una excepción específica indicando que el servicio de usuario no está disponible.
        throw new UserException(
                Constants.FALLBACK_USER_ID_FROM_EMAIL_ERROR + email, HttpStatus.SERVICE_UNAVAILABLE, t
        );
    }
}
