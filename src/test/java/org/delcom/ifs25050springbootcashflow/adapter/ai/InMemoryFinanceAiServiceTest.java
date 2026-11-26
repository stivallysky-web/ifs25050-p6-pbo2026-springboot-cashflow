package org.delcom.ifs25050springbootcashflow.adapter.ai;

import org.delcom.ifs25050springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25050springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25050springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryFinanceAiServiceTest {

    @Test
    void generatesDefaultTransactions() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        List<AiGeneratedTransaction> list = service.generateTransactions("Belanja");
        assertEquals(2, list.size());
    }

    @Test
    void usesCannedResponsesWhenProvided() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService(List.of(
                new AiGeneratedTransaction("Gaji", 5000000L, TransactionType.INCOME)
        ));
        service.addCannedResponse("Kopi", 25000L, TransactionType.EXPENSE);

        List<AiGeneratedTransaction> list = service.generateTransactions("Any");
        assertEquals(2, list.size());
        assertEquals("Gaji", list.get(0).description());
        assertEquals("Kopi", list.get(1).description());
    }

    @Test
    void handlesNullCannedListConstructor() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService(null);
        assertEquals(2, service.generateTransactions("Test").size());
    }

    @Test
    void rejectsBlankPrompt() {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        assertThrows(ValidationException.class, () -> service.generateTransactions(null));
        assertThrows(ValidationException.class, () -> service.generateTransactions("   "));
    }

    @Test
    void simulatesFailure() {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        service.setSimulateFailure(true);
        assertThrows(DomainException.class, () -> service.generateTransactions("Test"));
    }
}