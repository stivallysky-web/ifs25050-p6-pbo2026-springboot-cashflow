package org.delcom.ifs25050springbootcashflow.adapter.ai;

import org.delcom.ifs25050springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25050springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25050springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25050springbootcashflow.domain.repository.IFinanceAiService;

import java.util.ArrayList;
import java.util.List;

public class InMemoryFinanceAiService implements IFinanceAiService {

    private final List<AiGeneratedTransaction> cannedResponses = new ArrayList<>();
    private boolean simulateFailure = false;

    public InMemoryFinanceAiService() {
    }

    public InMemoryFinanceAiService(List<AiGeneratedTransaction> defaultResponses) {
        if (defaultResponses != null) {
            this.cannedResponses.addAll(defaultResponses);
        }
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public void addCannedResponse(String description, long amount, TransactionType type) {
        this.cannedResponses.add(new AiGeneratedTransaction(description, amount, type));
    }

    @Override
    public List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new ValidationException("Prompt instruksi tidak boleh kosong");
        }
        if (simulateFailure) {
            throw new DomainException("Simulasi kegagalan layanan AI");
        }
        if (!cannedResponses.isEmpty()) {
            return new ArrayList<>(cannedResponses);
        }
        return List.of(
                new AiGeneratedTransaction("Pemasukan AI: " + prompt.trim(), 500000L, TransactionType.INCOME),
                new AiGeneratedTransaction("Pengeluaran AI: " + prompt.trim(), 75000L, TransactionType.EXPENSE)
        );
    }
}