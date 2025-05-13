package com.accreditations_service.accreditations_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccreditationDataForPdf {
    private Long accreditationId;
    private String salePointName;
    private Long userId;
    private String userEmail;
    private Double amount;
    private LocalDateTime receiptDate;
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "AccreditationDataForPdf{" +
                "accreditationId=" + accreditationId +
                ", salePointName='" + salePointName + '\'' +
                ", userId=" + userId +
                ", userEmail='" + userEmail + '\'' +
                ", amount=" + amount +
                ", receiptDate=" + receiptDate +
                ", createdAt=" + createdAt +
                '}';
    }
}
