package org.delcom.ifs25050springbootcashflow.usecase;

import org.delcom.ifs25050springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25050springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25050springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25050springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FinanceUseCase {
    private final ITransactionRepository transactionRepository;

    public FinanceUseCase(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getTransactionsByUser(int userId) {
        return listTransactions(userId, null, TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST);
    }

    public List<Transaction> listTransactions(
            int userId,
            String keyword,
            TransactionTypeFilter typeFilter,
            SortOption sortOption
    ) {
        TransactionTypeFilter filter = typeFilter == null ? TransactionTypeFilter.ALL : typeFilter;
        SortOption sort = sortOption == null ? SortOption.NEWEST_FIRST : sortOption;

        Stream<Transaction> stream = transactionRepository.findByUserId(userId).stream()
                .filter(filter::matches);

        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.trim().toLowerCase();
            stream = stream.filter(t -> t.getDescription().toLowerCase().contains(lower));
        }

        return stream.sorted(sort.comparator()).toList();
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction addTransaction(int userId, String description, long amount, TransactionType type)
            throws ValidationException {
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Keterangan transaksi tidak boleh kosong");
        }
        if (amount <= 0) {
            throw new ValidationException("Jumlah transaksi harus lebih dari 0");
        }
        if (type == null) {
            throw new ValidationException("Tipe transaksi tidak boleh kosong");
        }
        return transactionRepository.save(userId, description.trim(), amount, type);
    }

    public void removeTransaction(int id) throws EntityNotFoundException {
        boolean deleted = transactionRepository.deleteById(id);
        if (!deleted) {
            throw new EntityNotFoundException(id);
        }
    }

    public void removeTransactionForUser(int userId, int id) throws EntityNotFoundException, ValidationException {
        Transaction transaction = requireOwnedTransaction(userId, id);
        removeTransaction(transaction.getId());
    }

    public void updateTransaction(
            int id,
            String description,
            Long amount,
            TransactionType type,
            String proofPath,
            boolean clearProof
    ) throws EntityNotFoundException, ValidationException {
        Optional<Transaction> found = transactionRepository.findById(id);
        if (found.isEmpty()) {
            throw new EntityNotFoundException(id);
        }
        Transaction transaction = found.get();
        applyUpdates(transaction, description, amount, type, proofPath, clearProof);
        transactionRepository.update(transaction);
    }

    public void updateTransactionForUser(
            int userId,
            int id,
            String description,
            Long amount,
            TransactionType type,
            String proofPath,
            boolean clearProof
    ) throws EntityNotFoundException, ValidationException {
        Transaction transaction = requireOwnedTransaction(userId, id);
        applyUpdates(transaction, description, amount, type, proofPath, clearProof);
        transactionRepository.update(transaction);
    }

    public long getTotalIncome(int userId) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    public long getTotalExpense(int userId) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    public long getBalance(int userId) {
        return transactionRepository.findByUserId(userId).stream()
                .mapToLong(Transaction::signedAmount)
                .sum();
    }

    private Transaction requireOwnedTransaction(int userId, int id)
            throws EntityNotFoundException, ValidationException {
        Optional<Transaction> found = transactionRepository.findById(id);
        if (found.isEmpty()) {
            throw new EntityNotFoundException(id);
        }
        Transaction transaction = found.get();
        if (transaction.getUserId() != userId) {
            throw new ValidationException("Transaksi tidak milik pengguna ini");
        }
        return transaction;
    }

    private void applyUpdates(
            Transaction transaction,
            String description,
            Long amount,
            TransactionType type,
            String proofPath,
            boolean clearProof
    ) throws ValidationException {
        if (description != null) {
            if (description.trim().isEmpty()) {
                throw new ValidationException("Keterangan transaksi tidak boleh kosong");
            }
            transaction.changeDescription(description.trim());
        }
        if (amount != null) {
            if (amount <= 0) {
                throw new ValidationException("Jumlah transaksi harus lebih dari 0");
            }
            transaction.changeAmount(amount);
        }
        if (type != null) {
            transaction.changeType(type);
        }
        if (clearProof) {
            transaction.changeProofPath(null);
        } else if (proofPath != null) {
            transaction.changeProofPath(proofPath);
        }
    }
}