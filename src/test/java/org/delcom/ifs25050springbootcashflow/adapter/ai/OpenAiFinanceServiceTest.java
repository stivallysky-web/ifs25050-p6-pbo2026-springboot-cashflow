package org.delcom.ifs25050springbootcashflow.adapter.ai;

import com.sun.net.httpserver.HttpServer;
import org.delcom.ifs25050springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25050springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25050springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiFinanceServiceTest {

    private HttpServer server;
    private String serverUrl;
    private String mockResponseBody;
    private int mockResponseCode;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] bytes = mockResponseBody != null ? mockResponseBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.sendResponseHeaders(mockResponseCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        serverUrl = "http://localhost:" + server.getAddress().getPort();
        mockResponseCode = 200;
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generatesTransactionsSuccessfully() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "[{\\"description\\": \\"Gaji\\", \\"amount\\": 5000000, \\"type\\": \\"INCOME\\"}, {\\"description\\": \\"Kopi\\", \\"amount\\": 25000, \\"type\\": \\"EXPENSE\\"}]"
                      }
                    }
                  ]
                }
                """;

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "test-model");
        List<AiGeneratedTransaction> list = service.generateTransactions("Catatan");
        assertEquals(2, list.size());
        assertEquals(TransactionType.INCOME, list.get(0).type());
        assertEquals(TransactionType.EXPENSE, list.get(1).type());
    }

    @Test
    void handlesMarkdownFencesSkipsInvalidItemsAndFallbackModel() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```json\\n[{}, {\\"description\\": \\" \\", \\"amount\\": 1000, \\"type\\": \\"INCOME\\"}, {\\"description\\": \\"A\\", \\"amount\\": null, \\"type\\": \\"INCOME\\"}, {\\"description\\": \\"B\\", \\"amount\\": 1000, \\"type\\": null}, {\\"description\\": \\"C\\", \\"amount\\": 0, \\"type\\": \\"INCOME\\"}, {\\"description\\": \\"D\\", \\"amount\\": 1000, \\"type\\": \\"UNKNOWN\\"}, {\\"description\\": \\"Valid\\", \\"amount\\": 15000, \\"type\\": \\"expense\\"}]\\n```"
                      }
                    }
                  ]
                }
                """;

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl + "/", "valid-key", "   ");
        List<AiGeneratedTransaction> list = service.generateTransactions("Catatan");
        assertEquals(1, list.size());
        assertEquals("Valid", list.get(0).description());
        assertEquals(15000L, list.get(0).amount());
        assertEquals(TransactionType.EXPENSE, list.get(0).type());
    }

    @Test
    void handlesCodeFenceWithoutNewlineAndWithoutClosingFence() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```[{\\"description\\": \\"Kopi\\", \\"amount\\": 20000, \\"type\\": \\"EXPENSE\\"}]"
                      }
                    }
                  ]
                }
                """;

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "model");
        assertEquals(1, service.generateTransactions("Catatan").size());
    }

    @Test
    void handlesCodeFenceWithoutNewlineWithClosingFence() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```[{\\"description\\": \\"Bonus\\", \\"amount\\": 100000, \\"type\\": \\"INCOME\\"}]```"
                      }
                    }
                  ]
                }
                """;

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl + "/chat/completions", "valid-key", null);
        assertEquals(1, service.generateTransactions("Catatan").size());
    }

    @Test
    void rejectsInvalidInputs() {
        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost", "valid-key", "model");
        assertThrows(ValidationException.class, () -> service.generateTransactions(null));
        assertThrows(ValidationException.class, () -> service.generateTransactions("   "));

        OpenAiFinanceService nullEp = new OpenAiFinanceService(null, "valid-key", "model");
        assertThrows(ValidationException.class, () -> nullEp.generateTransactions("Prompt"));

        OpenAiFinanceService blankEp = new OpenAiFinanceService("   ", "valid-key", "model");
        assertThrows(ValidationException.class, () -> blankEp.generateTransactions("Prompt"));

        OpenAiFinanceService nullKey = new OpenAiFinanceService("http://localhost", null, "model");
        assertThrows(ValidationException.class, () -> nullKey.generateTransactions("Prompt"));

        OpenAiFinanceService blankKey = new OpenAiFinanceService("http://localhost", "   ", "model");
        assertThrows(ValidationException.class, () -> blankKey.generateTransactions("Prompt"));
    }

    @Test
    void throwsOnHttpErrorAndMalformedResponses() {
        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "test-model");

        mockResponseCode = 500;
        mockResponseBody = "Internal Server Error";
        DomainException httpEx = assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));
        assertNotNull(httpEx.getMessage());

        mockResponseCode = 200;
        mockResponseBody = "{}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": []}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": [{}]}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": [{\"message\": {}}]}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": [{\"message\": {\"content\": \"{\\\"single\\\": 1}\"}}]}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": [{\"message\": {\"content\": \"[{\\\"description\\\": \\\"\\\"}]\"}}]}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));

        mockResponseBody = "{\"choices\": [{\"message\": {\"content\": \"invalid json\"}}]}";
        assertThrows(DomainException.class, () -> service.generateTransactions("Catatan"));
    }

    @Test
    void handlesHttpClientExceptionsAndSerializationError() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Connection refused"));

        OpenAiFinanceService service = new OpenAiFinanceService("http://localhost:1234", "key", "model", mockClient, new ObjectMapper());
        assertThrows(DomainException.class, () -> service.generateTransactions("Prompt"));

        when(mockClient.send(any(HttpRequest.class), any()))
                .thenThrow(new InterruptedException("Interrupted"));
        assertThrows(DomainException.class, () -> service.generateTransactions("Prompt"));

        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failure"));
        OpenAiFinanceService serFail = new OpenAiFinanceService("http://localhost:1234", "key", "model", HttpClient.newHttpClient(), mockMapper);
        assertThrows(DomainException.class, () -> serFail.generateTransactions("Prompt"));
    }

    @Test
    void skipsItemsWithMissingOrBlankFields() throws Exception {
        mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "[{}, {\\"description\\": \\"   \\", \\"amount\\": 1000, \\"type\\": \\"INCOME\\"}, {\\"description\\": \\"Tanpa Nominal\\", \\"type\\": \\"INCOME\\"}, {\\"description\\": \\"Tanpa Tipe\\", \\"amount\\": 1000}, {\\"description\\": \\"Bonus\\", \\"amount\\": 500000, \\"type\\": \\"INCOME\\"}]"
                      }
                    }
                  ]
                }
                """;

        OpenAiFinanceService service = new OpenAiFinanceService(serverUrl, "valid-key", "test-model");
        List<AiGeneratedTransaction> items = service.generateTransactions("Catat bonus");
        assertEquals(1, items.size());
    }
}