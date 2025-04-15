package com.accreditations_service.accreditations_service.dtos;

import java.time.LocalDateTime;

public record CreateAccreditationRequest(Long salePointId, Double amount, LocalDateTime receiptDate) {
}
