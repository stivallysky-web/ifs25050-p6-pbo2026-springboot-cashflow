package org.delcom.ifs25050springbootcashflow.domain.entity;

public enum TransactionType {
    INCOME,
    EXPENSE;

    public String label() {
        return switch (this) {
            case INCOME -> "Pemasukan";
            case EXPENSE -> "Pengeluaran";
        };
    }
}