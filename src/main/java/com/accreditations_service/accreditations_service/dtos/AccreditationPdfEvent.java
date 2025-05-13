package com.accreditations_service.accreditations_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccreditationPdfEvent {
    private String to;
    private String subject;
    private String bodyHeader;
    private AccreditationDataForPdf accreditationData;

    @Override
    public String toString() {
        return "AccreditationPdfEvent{" +
                "to='" + to + '\'' +
                ", subject='" + subject + '\'' +
                ", bodyHeader='" + bodyHeader + '\'' +
                ", accreditationData=" + accreditationData +
                '}';
    }
}
