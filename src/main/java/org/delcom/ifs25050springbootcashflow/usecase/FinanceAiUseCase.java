package org.delcom.ifs25050springbootcashflow.usecase;

import org.delcom.ifs25050springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25050springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25050springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25050springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25050springbootcashflow.domain.repository.ITransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class FinanceAiUseCase {

    private final IFinanceAiService aiService;
    private final ITransactionRepository transactionRepository;

    public FinanceAiUseCase(IFinanceAiService aiService, ITransactionRepository transactionRepository) {
        this.aiService = aiService;
        this.transactionRepository = transactionRepository;
    }

    public List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new ValidationException("Prompt instruksi tidak boleh kosong");
        }
        return aiService.generateTransactions(prompt.trim());
    }

    public List<Transaction> saveGeneratedTransactions(int userId, List<AiGeneratedTransaction> items)
            throws ValidationException {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Daftar transaksi hasil generate tidak boleh kosong");
        }
        List<Transaction> saved = new ArrayList<>();
        for (AiGeneratedTransaction item : items) {
            saved.add(transactionRepository.save(userId, item.description(), item.amount(), item.type()));
        }
        return saved;
    }
}