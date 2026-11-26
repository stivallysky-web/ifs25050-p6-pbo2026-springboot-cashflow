package org.delcom.ifs25050springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiGeneratedTransactionTest {

    @Test
    void createsValidAiGeneratedTransaction() {
        AiGeneratedTransaction item = new AiGeneratedTransaction("  Beli kopi  ", 25000L, TransactionType.EXPENSE);
        assertEquals("Beli kopi", item.description());
        assertEquals(25000L, item.amount());
        assertEquals(TransactionType.EXPENSE, item.type());
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction(null, 1000L, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("   ", 1000L, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("Kopi", 0L, TransactionType.EXPENSE));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("Kopi", -100L, TransactionType.EXPENSE));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("Kopi", 1000L, null));
    }
}