package org.delcom.ifs25050springbootcashflow.domain.entity;

import java.time.LocalDateTime;

public class Transaction {
    private final int id;
    private final int userId;
    private String description;
    private long amount;
    private TransactionType type;
    private String proofPath;
    private final LocalDateTime createdAt;

    public Transaction(int id, int userId, String description, long amount, TransactionType type) {
        this(id, userId, description, amount, type, null, LocalDateTime.now());
    }

    public Transaction(
            int id,
            int userId,
            String description,
            long amount,
            TransactionType type,
            String proofPath,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.description = description == null ? "" : description;
        this.amount = amount;
        this.type = type;
        this.proofPath = proofPath;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public long getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getProofPath() {
        return proofPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public long signedAmount() {
        return type == TransactionType.INCOME ? amount : -amount;
    }

    public void changeDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public void changeAmount(long amount) {
        this.amount = amount;
    }

    public void changeType(TransactionType type) {
        this.type = type;
    }

    public void changeProofPath(String proofPath) {
        this.proofPath = proofPath;
    }
}