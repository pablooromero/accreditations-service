package com.accreditations_service.accreditations_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Accreditation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long salePointId;

    @Column
    private Long userId;

    @Column
    private Double amount;

    @Column
    private String salePointName;

    @Column
    private LocalDateTime receiptDate;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;
}
