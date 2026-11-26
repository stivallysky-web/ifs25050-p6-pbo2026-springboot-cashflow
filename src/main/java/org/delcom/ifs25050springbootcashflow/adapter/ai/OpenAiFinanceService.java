package org.delcom.ifs25050springbootcashflow.adapter.ai;

import org.delcom.ifs25050springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25050springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25050springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25050springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25050springbootcashflow.domain.repository.IFinanceAiService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenAiFinanceService implements IFinanceAiService {

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiFinanceService(String endpoint, String apiKey, String model) {
        this(endpoint, apiKey, model, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), new ObjectMapper());
    }

    public OpenAiFinanceService(String endpoint, String apiKey, String model, HttpClient httpClient, ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException {
        if (prompt == null || prompt.isBlank()) {
            throw new ValidationException("Prompt instruksi tidak boleh kosong");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new ValidationException("Konfigurasi OPENAI_ENDPOINT belum diatur");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ValidationException("Konfigurasi OPENAI_KEY belum diatur");
        }

        String targetUrl = normalizeEndpoint(endpoint.trim());
        String requestJson = buildRequestBody(prompt.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException("Pemanggilan API AI terinterupsi: " + e.getMessage());
        } catch (IOException e) {
            throw new DomainException("Gagal menghubungi server AI: " + e.getMessage());
        }

        if (response.statusCode() != 200) {
            throw new DomainException("Layanan AI mengembalikan error (" + response.statusCode() + "): " + response.body());
        }

        return parseAiResponse(response.body());
    }

    private String normalizeEndpoint(String ep) {
        String base = ep.endsWith("/") ? ep.substring(0, ep.length() - 1) : ep;
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }

    private String buildRequestBody(String prompt) throws DomainException {
        try {
            String systemInstruction = "Anda adalah asisten pencatatan keuangan pribadi. "
                    + "Tugas Anda adalah mengubah catatan keuangan pengguna menjadi daftar transaksi terstruktur. "
                    + "Kembalikan HANYA JSON array valid tanpa markdown formatting atau teks tambahan. "
                    + "Setiap elemen array wajib memiliki key 'description' (string keterangan), "
                    + "'amount' (angka bulat positif dalam rupiah), dan 'type' ('INCOME' atau 'EXPENSE'). "
                    + "Contoh format: [{\"description\":\"Gaji bulanan\",\"amount\":5000000,\"type\":\"INCOME\"},"
                    + "{\"description\":\"Beli kopi\",\"amount\":25000,\"type\":\"EXPENSE\"}]";

            String selectedModel = (model == null || model.isBlank()) ? "gpt-4o-mini" : model.trim();
            Map<String, Object> payload = Map.of(
                    "model", selectedModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemInstruction),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3
            );
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new DomainException("Gagal menyusun request AI: " + e.getMessage());
        }
    }

    private List<AiGeneratedTransaction> parseAiResponse(String responseBody) throws DomainException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new DomainException("Respon AI tidak memiliki pilihan (choices)");
            }

            JsonNode message = choices.get(0).get("message");
            if (message == null || message.get("content") == null) {
                throw new DomainException("Konten respon AI kosong");
            }

            String content = message.get("content").asText().trim();
            content = stripMarkdownCodeFence(content);

            JsonNode arrayNode = objectMapper.readTree(content);
            if (!arrayNode.isArray()) {
                throw new DomainException("Format konten respon AI bukan merupakan JSON array");
            }

            List<AiGeneratedTransaction> result = new ArrayList<>();
            for (JsonNode item : arrayNode) {
                JsonNode descNode = item.get("description");
                JsonNode amountNode = item.get("amount");
                JsonNode typeNode = item.get("type");
                if (descNode == null || descNode.asText().isBlank() || amountNode == null || typeNode == null) {
                    continue;
                }
                long amount = amountNode.asLong(0);
                if (amount <= 0) {
                    continue;
                }
                TransactionType type = parseType(typeNode.asText());
                if (type == null) {
                    continue;
                }
                result.add(new AiGeneratedTransaction(descNode.asText().trim(), amount, type));
            }

            if (result.isEmpty()) {
                throw new DomainException("AI tidak menghasilkan daftar transaksi yang valid");
            }

            return result;
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Gagal memproses respon dari AI: " + e.getMessage());
        }
    }

    private TransactionType parseType(String raw) {
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if ("INCOME".equals(upper)) {
            return TransactionType.INCOME;
        }
        if ("EXPENSE".equals(upper)) {
            return TransactionType.EXPENSE;
        }
        return null;
    }

    private String stripMarkdownCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            trimmed = (firstNewline != -1) ? trimmed.substring(firstNewline + 1) : trimmed.substring(3);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}