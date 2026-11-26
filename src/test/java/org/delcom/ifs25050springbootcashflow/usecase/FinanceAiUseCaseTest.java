package org.delcom.ifs25050springbootcashflow.usecase;

import org.delcom.ifs25050springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25050springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25050springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25050springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceAiUseCaseTest {

    private InMemoryFinanceAiService aiService;
    private TransactionRepository transactionRepository;
    private FinanceAiUseCase useCase;

    @BeforeEach
    void setUp() {
        aiService = new InMemoryFinanceAiService();
        transactionRepository = new TransactionRepository();
        useCase = new FinanceAiUseCase(aiService, transactionRepository);
    }

    @Test
    void generateTransactionsReturnsList() throws Exception {
        List<AiGeneratedTransaction> result = useCase.generateTransactions("Catat pengeluaran hari ini");
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
    }

    @Test
    void generateTransactionsRejectsBlankPrompt() {
        assertThrows(ValidationException.class, () -> useCase.generateTransactions(null));
        assertThrows(ValidationException.class, () -> useCase.generateTransactions("   "));
    }

    @Test
    void saveGeneratedTransactionsPersistsToRepository() throws Exception {
        List<AiGeneratedTransaction> items = List.of(
                new AiGeneratedTransaction("Gaji", 5000000L, TransactionType.INCOME),
                new AiGeneratedTransaction("Kopi", 25000L, TransactionType.EXPENSE)
        );

        List<Transaction> saved = useCase.saveGeneratedTransactions(1, items);
        assertEquals(2, saved.size());
        assertEquals(2, transactionRepository.findByUserId(1).size());
    }

    @Test
    void saveGeneratedTransactionsRejectsEmptyOrNullItems() {
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTransactions(1, null));
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTransactions(1, Collections.emptyList()));
    }
}