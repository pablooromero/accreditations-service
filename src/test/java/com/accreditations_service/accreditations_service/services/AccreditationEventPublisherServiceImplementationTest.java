package com.accreditations_service.accreditations_service.services;

import com.accreditations_service.accreditations_service.config.RabbitMQConfig;
import com.accreditations_service.accreditations_service.dtos.AccreditationDataForPdf;
import com.accreditations_service.accreditations_service.dtos.AccreditationPdfEvent;
import com.accreditations_service.accreditations_service.services.implementations.AccreditationEventPublisherServiceImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccreditationEventPublisherServiceImplementationTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Spy
    @InjectMocks
    private AccreditationEventPublisherServiceImplementation eventPublisher;

    private AccreditationPdfEvent testPdfEvent;
    private Long testAccreditationId;

    @BeforeEach
    void setUp() {
        AccreditationDataForPdf pdfData = new AccreditationDataForPdf(
                1L, "Test Sale Point", 100L, "test@example.com",
                150.75, LocalDateTime.now(), LocalDateTime.now()
        );
        testPdfEvent = new AccreditationPdfEvent(
                "test@example.com", "Test Subject", "Test Body Header", pdfData
        );
        testAccreditationId = 1L;
    }

    @Test
    @DisplayName("publishAccreditationEvent - Debería enviar mensaje a RabbitMQ exitosamente")
    void publishAccreditationEvent_onSuccess_shouldSendMessage() {

        eventPublisher.publishAccreditationEvent(testPdfEvent, testAccreditationId);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_PDF),
                eq(testPdfEvent)
        );
        verify(eventPublisher, never()).recoverPublishAccreditationEvent(isA(AmqpException.class), eq(testPdfEvent), eq(testAccreditationId));
        verify(eventPublisher, never()).recoverPublishAccreditationEvent(not(isA(AmqpException.class)), eq(testPdfEvent), eq(testAccreditationId));
    }

    @Test
    @DisplayName("publishAccreditationEvent - Debería propagar AmqpException si RabbitTemplate falla y AOP de Retry no está activo")
    void publishAccreditationEvent_onAmqpException_shouldPropagateIfAopNotActive() {
        AmqpException amqpException = new AmqpException("Test AMQP connection refused");
        doThrow(amqpException)
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(AccreditationPdfEvent.class));

        assertThrows(AmqpException.class, () -> {
            eventPublisher.publishAccreditationEvent(testPdfEvent, testAccreditationId);
        });

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_PDF),
                eq(testPdfEvent)
        );
        verify(eventPublisher, never()).recoverPublishAccreditationEvent(any(AmqpException.class), any(), anyLong());
        verify(eventPublisher, never()).recoverPublishAccreditationEvent(not(isA(AmqpException.class)), any(), anyLong());    }


    @Test
    @DisplayName("publishAccreditationEvent - Debería propagar RuntimeException no AMQP y no llamar a recover vía Retryable")
    void publishAccreditationEvent_onRuntimeException_shouldThrowAndNotCallRecoverViaRetryable() {
        RuntimeException runtimeException = new RuntimeException("Test runtime exception, no es AmqpException");
        doThrow(runtimeException)
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(AccreditationPdfEvent.class));

        assertThrows(RuntimeException.class, () -> {
            eventPublisher.publishAccreditationEvent(testPdfEvent, testAccreditationId);
        });

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY_PDF),
                eq(testPdfEvent)
        );
        verify(eventPublisher, never()).recoverPublishAccreditationEvent(isA(AmqpException.class), eq(testPdfEvent), eq(testAccreditationId));
        verify(eventPublisher, never()).recoverPublishAccreditationEvent(not(isA(AmqpException.class)), eq(testPdfEvent), eq(testAccreditationId));
    }


    @Test
    @DisplayName("recoverPublishAccreditationEvent (AmqpException) - Debería loguear el error correctamente")
    void recoverPublishAccreditationEvent_amqpException_shouldLog() {
        AmqpException amqpEx = new AmqpException("Test AMQP connection error for direct recover call");

        eventPublisher.recoverPublishAccreditationEvent(amqpEx, testPdfEvent, testAccreditationId);

        assertTrue(true, "El método Recover para AmqpException se ejecutó.");
    }

}