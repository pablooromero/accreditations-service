package com.accreditations_service.accreditations_service.services.implementations;

import com.accreditations_service.accreditations_service.config.RabbitMQConfig;
import com.accreditations_service.accreditations_service.dtos.AccreditationPdfEvent;
import com.accreditations_service.accreditations_service.services.AccreditationEventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccreditationEventPublisherServiceImplementation implements AccreditationEventPublisherService {
    private final RabbitTemplate rabbitTemplate;

    @Retryable(
            value = {org.springframework.amqp.AmqpException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @Override
    public void publishAccreditationEvent(AccreditationPdfEvent pdfEvent, Long accreditationId) {
        log.info("Intentando enviar evento PDF a RabbitMQ para accreditación con ID: " + accreditationId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_PDF, pdfEvent);
        log.info("Evento de PDF para acreditación ID: {} enviado exitosamente a RabbitMQ.", accreditationId);
    }

    @Recover
    @Override
    public void recoverPublishAccreditationEvent(org.springframework.amqp.AmqpException e, AccreditationPdfEvent pdfEvent, Long accreditationId) {
        log.error("FALLO DEFINITIVO al enviar evento de PDF a RabbitMQ para acreditación ID {} después de múltiples intentos. Causa: {}",
                accreditationId, e.getMessage(), e);
    }

    @Recover
    @Override
    public void recoverPublishAccreditationPdfEvent(Exception e, AccreditationPdfEvent pdfEvent, Long accreditationId) {
        log.error("FALLO DEFINITIVO INESPERADO al enviar evento de PDF a RabbitMQ para acreditación ID {} después de múltiples intentos. Causa: {}",
                accreditationId, e.getMessage(), e);
    }
}
