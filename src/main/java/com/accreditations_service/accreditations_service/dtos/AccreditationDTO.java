package com.accreditations_service.accreditations_service.dtos;

import java.time.LocalDateTime;

public record AccreditationDTO(Long id, Long salePointId, Long userId, String salePointName, Double amount, LocalDateTime receiptDate) {
}
