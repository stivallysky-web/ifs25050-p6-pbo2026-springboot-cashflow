package org.delcom.ifs25050springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortOptionTest {

    @Test
    void sortsByDescriptionAndAmountAndDate() {
        LocalDateTime older = LocalDateTime.of(2026, 1, 1, 8, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 1, 2, 8, 0);

        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(1, 1, "Beta", 50000L, TransactionType.EXPENSE, null, older));
        list.add(new Transaction(2, 1, "Alpha", 100000L, TransactionType.INCOME, null, newer));

        list.sort(SortOption.NEWEST_FIRST.comparator());
        assertEquals("Alpha", list.get(0).getDescription());

        list.sort(SortOption.OLDEST_FIRST.comparator());
        assertEquals("Beta", list.get(0).getDescription());

        list.sort(SortOption.AMOUNT_DESC.comparator());
        assertEquals(100000L, list.get(0).getAmount());

        list.sort(SortOption.AMOUNT_ASC.comparator());
        assertEquals(50000L, list.get(0).getAmount());

        list.sort(SortOption.DESCRIPTION_ASC.comparator());
        assertEquals("Alpha", list.get(0).getDescription());

        list.sort(SortOption.DESCRIPTION_DESC.comparator());
        assertEquals("Beta", list.get(0).getDescription());
    }

    @Test
    void labelsAreReadable() {
        assertEquals("Terbaru", SortOption.NEWEST_FIRST.label());
        assertEquals("Terlama", SortOption.OLDEST_FIRST.label());
        assertEquals("Jumlah Terbesar", SortOption.AMOUNT_DESC.label());
        assertEquals("Jumlah Terkecil", SortOption.AMOUNT_ASC.label());
        assertEquals("Keterangan A-Z", SortOption.DESCRIPTION_ASC.label());
        assertEquals("Keterangan Z-A", SortOption.DESCRIPTION_DESC.label());
    }
}