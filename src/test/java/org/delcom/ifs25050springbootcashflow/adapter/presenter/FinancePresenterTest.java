package org.delcom.ifs25050springbootcashflow.adapter.presenter;

import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancePresenterTest {

    private FinancePresenter presenter;

    @BeforeEach
    void setUp() {
        presenter = new FinancePresenter();
    }

    @Test
    void formatsAmountBalanceAndTransaction() {
        assertEquals("Rp 1,000,000.00", presenter.formatAmount(1000000L));
        assertEquals("-Rp 25,000.00", presenter.formatAmount(-25000L));
        assertEquals("Saldo: Rp 4,975,000.00", presenter.formatBalance(4975000L));

        LocalDateTime created = LocalDateTime.of(2026, 11, 1, 8, 0);
        Transaction t = new Transaction(1, 1, "Gaji", 1000000L, TransactionType.INCOME, null, created);
        assertEquals("[1] Gaji | Rp 1,000,000.00 | [Pemasukan]", presenter.format(t));
        assertEquals("01/11/2026 08:00", presenter.formatCreatedAt(t));
    }

    @Test
    void showMessages() {
        Transaction t = new Transaction(1, 1, "Kopi", 25000L, TransactionType.EXPENSE);
        assertTrue(presenter.showAddSuccess(t).contains("Berhasil menambah transaksi"));
        assertEquals("Berhasil menghapus transaksi.", presenter.showRemoveSuccess());
        assertEquals("[!] Gagal menghapus transaksi dengan ID: 9.", presenter.showRemoveFailed(9));
        assertEquals("Berhasil mengubah transaksi.", presenter.showUpdateSuccess());
        assertEquals("[!] Gagal mengubah transaksi dengan ID: 8.", presenter.showUpdateFailed(8));
        assertEquals("[!] keterangan kosong", presenter.showValidationError("keterangan kosong"));
    }
}