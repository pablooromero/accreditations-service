package com.accreditations_service.accreditations_service.services;

import com.accreditations_service.accreditations_service.dtos.AccreditationPdfEvent;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

public interface AccreditationEventPublisherService {
    @Retryable(
            value = {org.springframework.amqp.AmqpException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    void publishAccreditationEvent(AccreditationPdfEvent pdfEvent, Long accreditationId);

    @Recover
    void recoverPublishAccreditationEvent(org.springframework.amqp.AmqpException e, AccreditationPdfEvent pdfEvent, Long accreditationId);

    @Recover
    void recoverPublishAccreditationPdfEvent(Exception e, AccreditationPdfEvent pdfEvent, Long accreditationId);
}
