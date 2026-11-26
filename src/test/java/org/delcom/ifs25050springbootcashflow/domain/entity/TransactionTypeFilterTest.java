package org.delcom.ifs25050springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTypeFilterTest {

    @Test
    void matchesByType() {
        Transaction income = new Transaction(1, 1, "Gaji", 1000L, TransactionType.INCOME);
        Transaction expense = new Transaction(2, 1, "Kopi", 500L, TransactionType.EXPENSE);

        assertTrue(TransactionTypeFilter.ALL.matches(income));
        assertTrue(TransactionTypeFilter.ALL.matches(expense));
        assertTrue(TransactionTypeFilter.INCOME.matches(income));
        assertFalse(TransactionTypeFilter.INCOME.matches(expense));
        assertTrue(TransactionTypeFilter.EXPENSE.matches(expense));
        assertFalse(TransactionTypeFilter.EXPENSE.matches(income));
    }

    @Test
    void labelsAreReadable() {
        assertEquals("Semua tipe", TransactionTypeFilter.ALL.label());
        assertEquals("Pemasukan", TransactionTypeFilter.INCOME.label());
        assertEquals("Pengeluaran", TransactionTypeFilter.EXPENSE.label());
    }
}