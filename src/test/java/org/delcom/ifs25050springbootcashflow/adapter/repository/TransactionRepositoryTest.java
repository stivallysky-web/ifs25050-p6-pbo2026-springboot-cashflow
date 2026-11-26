package org.delcom.ifs25050springbootcashflow.adapter.repository;

import org.delcom.ifs25050springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRepositoryTest {

    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
    }

    @Test
    void saveAssignsIncrementalIdsAndFiltersByUser() {
        Transaction first = repository.save(1, "Gaji", 1000L, TransactionType.INCOME);
        Transaction second = repository.save(1, "Kopi", 500L, TransactionType.EXPENSE);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
        assertEquals(2, repository.findAll().size());
        assertEquals(2, repository.findByUserId(1).size());
        assertTrue(repository.findByUserId(2).isEmpty());
    }

    @Test
    void findByIdDeleteUpdateAndDefensiveCopy() {
        Transaction t = repository.save(1, "Makan", 20000L, TransactionType.EXPENSE);
        Optional<Transaction> found = repository.findById(t.getId());
        assertTrue(found.isPresent());

        t.changeDescription("Makan Siang");
        repository.update(t);
        assertEquals("Makan Siang", repository.findById(t.getId()).orElseThrow().getDescription());

        List<Transaction> all = repository.findAll();
        all.clear();
        assertEquals(1, repository.findAll().size());

        assertTrue(repository.deleteById(t.getId()));
        assertFalse(repository.deleteById(t.getId()));
        assertTrue(repository.findById(t.getId()).isEmpty());
    }
}