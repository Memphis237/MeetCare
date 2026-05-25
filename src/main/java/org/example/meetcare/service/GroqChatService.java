package org.example.meetcare.service;

import org.example.meetcare.model.ChatbotMessage;
import org.example.meetcare.utils.EnvLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroqChatService {
    private static final String DEFAULT_MODEL = "llama-3.1-8b-instant";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final int HISTORY_LIMIT = 8;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String generateReply(String userMessage, List<ChatbotMessage> history) {
        EnvLoader.load();
        String apiKey = getConfig("GROQ_API_KEY", "groq.api.key");
        if (isBlank(apiKey)) {
            throw new IllegalStateException("La cle GROQ_API_KEY est manquante.");
        }

        String model = getConfig("GROQ_MODEL", "groq.model");
        if (isBlank(model)) {
            model = DEFAULT_MODEL;
        }

        String requestBody = buildRequestBody(userMessage, history, model);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Groq a renvoye le code " + response.statusCode() + ".");
            }
            return extractAssistantContent(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La requete Groq a ete interrompue.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de joindre Groq.", e);
        }
    }

    public String getModelName() {
        EnvLoader.load();
        String model = getConfig("GROQ_MODEL", "groq.model");
        return isBlank(model) ? DEFAULT_MODEL : model.trim();
    }

    private String buildRequestBody(String userMessage, List<ChatbotMessage> history, String model) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"model\":\"").append(escapeJson(model)).append("\",");
        builder.append("\"messages\":[");
        appendMessage(builder, "system", systemPrompt());

        List<ChatbotMessage> limitedHistory = history == null ? List.of()
                : history.subList(Math.max(0, history.size() - HISTORY_LIMIT), history.size());

        for (ChatbotMessage exchange : limitedHistory) {
            appendMessage(builder, "user", exchange.getQuestion());
            appendMessage(builder, "assistant", exchange.getResponse());
        }

        appendMessage(builder, "user", userMessage);
        trimTrailingComma(builder);
        builder.append("],");
        builder.append("\"temperature\":0.4,");
        builder.append("\"max_tokens\":700");
        builder.append("}");
        return builder.toString();
    }

    private void appendMessage(StringBuilder builder, String role, String content) {
        builder.append("{")
                .append("\"role\":\"").append(escapeJson(role)).append("\",")
                .append("\"content\":\"").append(escapeJson(content)).append("\"")
                .append("},");
    }

    private void trimTrailingComma(StringBuilder builder) {
        int lastIndex = builder.length() - 1;
        if (lastIndex >= 0 && builder.charAt(lastIndex) == ',') {
            builder.deleteCharAt(lastIndex);
        }
    }

    private String extractAssistantContent(String responseBody) {
        Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher matcher = pattern.matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("La reponse Groq ne contient pas de contenu.");
        }
        return unescapeJson(matcher.group(1));
    }

    private String unescapeJson(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaping = false;
        boolean unicode = false;
        StringBuilder unicodeBuffer = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (unicode) {
                unicodeBuffer.append(c);
                if (unicodeBuffer.length() == 4) {
                    int codePoint = Integer.parseInt(unicodeBuffer.toString(), 16);
                    result.append((char) codePoint);
                    unicode = false;
                    escaping = false;
                    unicodeBuffer.setLength(0);
                }
                continue;
            }

            if (escaping) {
                switch (c) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        unicode = true;
                        unicodeBuffer.setLength(0);
                    }
                    default -> result.append(c);
                }
                if (!unicode) {
                    escaping = false;
                }
                continue;
            }

            if (c == '\\') {
                escaping = true;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private String systemPrompt() {
        return """
                Tu es Assistant IA de MeetCare.
                Reponds en francais, avec un ton clair, rassurant et professionnel.
                Tu peux aider sur le suivi de soins, les rendez-vous et les messages generaux.
                Si une situation semble urgente ou medicalement sensible, conseille de contacter un professionnel de sante.
                """.trim();
    }

    private String getConfig(String envKey, String propertyKey) {
        String envStylePropertyValue = System.getProperty(envKey);
        if (!isBlank(envStylePropertyValue)) {
            return envStylePropertyValue;
        }

        String propertyValue = System.getProperty(propertyKey);
        if (!isBlank(propertyValue)) {
            return propertyValue;
        }

        String envValue = System.getenv(envKey);
        if (!isBlank(envValue)) {
            return envValue;
        }

        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
