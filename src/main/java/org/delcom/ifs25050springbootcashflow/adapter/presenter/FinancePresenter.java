package org.delcom.ifs25050springbootcashflow.adapter.presenter;

import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FinancePresenter {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String formatAmount(long amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        if (amount < 0) {
            return "-Rp " + formatter.format(Math.abs(amount));
        }
        return "Rp " + formatter.format(amount);
    }

    public String format(Transaction transaction) {
        return String.format(
                "[%d] %s | %s | [%s]",
                transaction.getId(),
                transaction.getDescription(),
                formatAmount(transaction.getAmount()),
                transaction.getType().label()
        );
    }

    public String formatBalance(long balance) {
        return "Saldo: " + formatAmount(balance);
    }

    public String formatCreatedAt(Transaction transaction) {
        return transaction.getCreatedAt().format(DATE_TIME_FORMAT);
    }

    public String showAddSuccess(Transaction transaction) {
        return "Berhasil menambah transaksi: " + format(transaction);
    }

    public String showRemoveSuccess() {
        return "Berhasil menghapus transaksi.";
    }

    public String showRemoveFailed(int id) {
        return String.format("[!] Gagal menghapus transaksi dengan ID: %d.", id);
    }

    public String showUpdateSuccess() {
        return "Berhasil mengubah transaksi.";
    }

    public String showUpdateFailed(int id) {
        return String.format("[!] Gagal mengubah transaksi dengan ID: %d.", id);
    }

    public String showValidationError(String message) {
        return "[!] " + message;
    }
}