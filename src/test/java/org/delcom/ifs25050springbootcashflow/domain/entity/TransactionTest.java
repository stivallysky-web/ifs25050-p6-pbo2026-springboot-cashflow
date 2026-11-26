package org.delcom.ifs25050springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionTest {

    @Test
    void constructorsGettersAndSignedAmount() {
        Transaction income = new Transaction(1, 10, "Gaji", 5000000L, TransactionType.INCOME);
        assertEquals(1, income.getId());
        assertEquals(10, income.getUserId());
        assertEquals("Gaji", income.getDescription());
        assertEquals(5000000L, income.getAmount());
        assertEquals(TransactionType.INCOME, income.getType());
        assertNull(income.getProofPath());
        assertNotNull(income.getCreatedAt());
        assertEquals(5000000L, income.signedAmount());

        LocalDateTime created = LocalDateTime.of(2026, 11, 1, 8, 0);
        Transaction expense = new Transaction(2, 10, null, 25000L, TransactionType.EXPENSE, "proofs/a.png", created);
        assertEquals("", expense.getDescription());
        assertEquals("proofs/a.png", expense.getProofPath());
        assertEquals(created, expense.getCreatedAt());
        assertEquals(-25000L, expense.signedAmount());
    }

    @Test
    void mutatorsUpdateFields() {
        Transaction t = new Transaction(1, 10, "Awal", 1000L, TransactionType.EXPENSE);
        t.changeDescription("Baru");
        t.changeAmount(2500L);
        t.changeType(TransactionType.INCOME);
        t.changeProofPath("proofs/b.png");

        assertEquals("Baru", t.getDescription());
        assertEquals(2500L, t.getAmount());
        assertEquals(TransactionType.INCOME, t.getType());
        assertEquals("proofs/b.png", t.getProofPath());

        t.changeDescription(null);
        assertEquals("", t.getDescription());
    }
}