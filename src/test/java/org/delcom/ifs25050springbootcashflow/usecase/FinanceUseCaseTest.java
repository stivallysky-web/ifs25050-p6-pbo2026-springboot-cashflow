package org.delcom.ifs25050springbootcashflow.usecase;

import org.delcom.ifs25050springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25050springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25050springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25050springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceUseCaseTest {

    private static final int USER_A = 1;
    private static final int USER_B = 2;

    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FinanceUseCase(new TransactionRepository());
    }

    @Test
    void addGetAndBalanceCalculations() throws ValidationException {
        Transaction t1 = useCase.addTransaction(USER_A, "Gaji", 5000000L, TransactionType.INCOME);
        useCase.addTransaction(USER_A, "Kopi", 25000L, TransactionType.EXPENSE);

        assertEquals(1, t1.getId());
        assertEquals(2, useCase.getTransactionsByUser(USER_A).size());
        assertTrue(useCase.getTransactionsByUser(USER_B).isEmpty());
        assertEquals(2, useCase.getAllTransactions().size());
        assertEquals(5000000L, useCase.getTotalIncome(USER_A));
        assertEquals(25000L, useCase.getTotalExpense(USER_A));
        assertEquals(4975000L, useCase.getBalance(USER_A));
    }

    @Test
    void addTransactionValidation() {
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, null, 1000L, TransactionType.INCOME));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, "  ", 1000L, TransactionType.INCOME));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, "Kopi", 0L, TransactionType.EXPENSE));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, "Kopi", -500L, TransactionType.EXPENSE));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER_A, "Kopi", 1000L, null));
    }

    @Test
    void removeTransactionSuccessAndValidation() throws Exception {
        Transaction t = useCase.addTransaction(USER_A, "Makan", 20000L, TransactionType.EXPENSE);
        assertThrows(ValidationException.class, () -> useCase.removeTransactionForUser(USER_B, t.getId()));
        useCase.removeTransactionForUser(USER_A, t.getId());
        assertTrue(useCase.getTransactionsByUser(USER_A).isEmpty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> useCase.removeTransaction(99));
        assertEquals(99, ex.getId());
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransactionForUser(USER_A, 99));
    }

    @Test
    void updateTransactionPartialAndValidation() throws Exception {
        Transaction t = useCase.addTransaction(USER_A, "Awal", 10000L, TransactionType.EXPENSE);

        useCase.updateTransactionForUser(USER_A, t.getId(), "Baru", 20000L, TransactionType.INCOME, "proofs/a.png", false);
        Transaction updated = useCase.getTransactionsByUser(USER_A).get(0);
        assertEquals("Baru", updated.getDescription());
        assertEquals(20000L, updated.getAmount());
        assertEquals(TransactionType.INCOME, updated.getType());
        assertEquals("proofs/a.png", updated.getProofPath());

        useCase.updateTransactionForUser(USER_A, t.getId(), null, null, null, null, true);
        assertNull(useCase.getTransactionsByUser(USER_A).get(0).getProofPath());

        useCase.updateTransaction(t.getId(), "Revisi", null, null, null, false);
        assertEquals("Revisi", useCase.getTransactionsByUser(USER_A).get(0).getDescription());

        assertThrows(ValidationException.class, () -> useCase.updateTransaction(t.getId(), " ", null, null, null, false));
        assertThrows(ValidationException.class, () -> useCase.updateTransaction(t.getId(), null, 0L, null, null, false));
        assertThrows(EntityNotFoundException.class, () -> useCase.updateTransaction(999, "X", null, null, null, false));
        assertThrows(ValidationException.class, () ->
                useCase.updateTransactionForUser(USER_B, t.getId(), "X", null, null, null, false));
        assertThrows(EntityNotFoundException.class, () ->
                useCase.updateTransactionForUser(USER_A, 999, "X", null, null, null, false));
    }

    @Test
    void listTransactionsFiltersAndDefaultsToNewest() throws Exception {
        Transaction first = useCase.addTransaction(USER_A, "Gaji", 5000000L, TransactionType.INCOME);
        Transaction second = useCase.addTransaction(USER_A, "Kopi", 25000L, TransactionType.EXPENSE);

        List<Transaction> newest = useCase.getTransactionsByUser(USER_A);
        assertEquals(second.getId(), newest.get(0).getId());

        List<Transaction> incomes = useCase.listTransactions(USER_A, null, TransactionTypeFilter.INCOME, SortOption.NEWEST_FIRST);
        assertEquals(1, incomes.size());
        assertEquals(first.getId(), incomes.get(0).getId());

        List<Transaction> search = useCase.listTransactions(USER_A, "kop", TransactionTypeFilter.EXPENSE, SortOption.AMOUNT_ASC);
        assertEquals(1, search.size());
        assertEquals("Kopi", search.get(0).getDescription());

        List<Transaction> blankSearch = useCase.listTransactions(USER_A, "   ", null, null);
        assertEquals(2, blankSearch.size());
    }
}