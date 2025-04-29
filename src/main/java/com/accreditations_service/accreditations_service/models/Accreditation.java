package com.accreditations_service.accreditations_service.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
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

    public Accreditation() {}

    public Accreditation(Long id, Long salePointId, Long userId, Double amount, String salePointName, LocalDateTime receiptDate, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.salePointId = salePointId;
        this.userId = userId;
        this.amount = amount;
        this.salePointName = salePointName;
        this.receiptDate = receiptDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Long getSalePointId() {
        return salePointId;
    }

    public void setSalePointId(Long salePointId) {
        this.salePointId = salePointId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getSalePointName() {
        return salePointName;
    }

    public void setSalePointName(String salePointName) {
        this.salePointName = salePointName;
    }

    public LocalDateTime getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDateTime receiptDate) {
        this.receiptDate = receiptDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
