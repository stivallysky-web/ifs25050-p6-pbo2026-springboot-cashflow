package org.delcom.ifs25050springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionTypeTest {

    @Test
    void labelsAreReadable() {
        assertEquals("Pemasukan", TransactionType.INCOME.label());
        assertEquals("Pengeluaran", TransactionType.EXPENSE.label());
    }
}