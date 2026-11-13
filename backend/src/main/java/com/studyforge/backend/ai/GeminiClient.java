package com.studyforge.backend.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.studyforge.backend.common.ApiException;
import com.studyforge.backend.common.JsonSupport;
import com.studyforge.backend.config.AppProperties;

/**
 * Client minimal pentru Gemini API (generateContent) care cere raspuns JSON structurat.
 * Docs: https://ai.google.dev/api/generate-content
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final AppProperties.Gemini props;
    private final JsonSupport json;
    private final RestClient restClient;

    public GeminiClient(AppProperties properties, JsonSupport json) {
        this.props = properties.gemini();
        this.json = json;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.timeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    public String model() {
        return props.model();
    }

    /**
     * Trimite prompt-ul si deserializeaza raspunsul JSON in tipul cerut.
     *
     * @param systemInstruction rolul/regulile modelului
     * @param prompt            cererea propriu-zisa (include si textul din PDF)
     * @param responseSchema    schema JSON (format OpenAPI al Gemini: type OBJECT/ARRAY/STRING/INTEGER)
     */
    public <T> T generateJson(String systemInstruction, String prompt, Map<String, Object> responseSchema,
            Class<T> type) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cheia Gemini nu este configurată. Setează GEMINI_API_KEY în fișierul .env și repornește backend-ul.");
        }

        String requestBody = json.write(buildRequest(systemInstruction, prompt, responseSchema));
        String responseText;
        try {
            responseText = callWithRetry(requestBody);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 400 && responseSchema != null) {
                // Unele modele resping anumite scheme; reincercam doar cu "application/json".
                log.warn("Gemini a respins schema ({}). Reincerc fara responseSchema.", shorten(e.getResponseBodyAsString()));
                try {
                    responseText = callWithRetry(json.write(buildRequest(systemInstruction, prompt, null)));
                } catch (RestClientResponseException retryError) {
                    throw translate(retryError);
                }
            } else {
                throw translate(e);
            }
        }

        String modelOutput = extractText(responseText);
        try {
            return json.read(stripCodeFences(modelOutput), type);
        } catch (RuntimeException e) {
            log.warn("Raspuns Gemini care nu e JSON valid: {}", shorten(modelOutput));
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Gemini a returnat un răspuns incomplet sau invalid. Încearcă din nou.", e);
        }
    }

    private Map<String, Object> buildRequest(String systemInstruction, String prompt,
            Map<String, Object> responseSchema) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.4);
        if (responseSchema != null) {
            generationConfig.put("responseSchema", responseSchema);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", prompt)))));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private String callWithRetry(String requestBody) {
        URI uri = URI.create(stripTrailingSlash(props.baseUrl()) + "/models/" + props.model() + ":generateContent");
        int attempt = 0;
        while (true) {
            try {
                return restClient.post()
                        .uri(uri)
                        .header("x-goog-api-key", props.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 429 || status == 500 || status == 503 || status == 504;
                if (!retryable || attempt >= props.maxRetries()) {
                    throw e;
                }
                log.info("Gemini a raspuns {}. Reincerc ({}/{})...", status, attempt + 1, props.maxRetries());
            } catch (ResourceAccessException e) {
                if (attempt >= props.maxRetries()) {
                    throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                            "Nu am putut contacta Gemini (timeout sau fără internet). Încearcă din nou.", e);
                }
                log.info("Eroare de retea catre Gemini ({}). Reincerc...", e.getMessage());
            }
            attempt++;
            sleep(Duration.ofSeconds(2L * attempt));
        }
    }

    private String extractText(String responseText) {
        GenerateContentResponse response;
        try {
            response = json.read(responseText, GenerateContentResponse.class);
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Răspuns neașteptat de la Gemini.", e);
        }
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            String reason = response != null && response.promptFeedback() != null
                    ? response.promptFeedback().blockReason()
                    : null;
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Gemini nu a generat niciun răspuns" + (reason != null ? " (motiv: " + reason + ")." : "."));
        }
        Candidate candidate = response.candidates().get(0);
        List<String> texts = new ArrayList<>();
        if (candidate.content() != null && candidate.content().parts() != null) {
            for (Part part : candidate.content().parts()) {
                if (part != null && part.text() != null && !Boolean.TRUE.equals(part.thought())) {
                    texts.add(part.text());
                }
            }
        }
        String text = String.join("", texts).trim();
        if (text.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Gemini a returnat un răspuns gol (finishReason: " + candidate.finishReason() + "). Încearcă din nou.");
        }
        if ("MAX_TOKENS".equals(candidate.finishReason())) {
            log.warn("Raspunsul Gemini a fost taiat (MAX_TOKENS).");
        }
        return text;
    }

    private ApiException translate(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        log.warn("Eroare Gemini {}: {}", status, shorten(e.getResponseBodyAsString()));
        return switch (status) {
            case 400 -> new ApiException(HttpStatus.BAD_GATEWAY,
                    "Gemini a respins cererea (400). Verifică numele modelului (GEMINI_MODEL=" + props.model() + ").", e);
            case 401, 403 -> new ApiException(HttpStatus.BAD_GATEWAY,
                    "Cheia Gemini este invalidă sau nu are acces la modelul " + props.model() + ".", e);
            case 404 -> new ApiException(HttpStatus.BAD_GATEWAY,
                    "Modelul Gemini \"" + props.model() + "\" nu există. Schimbă GEMINI_MODEL în .env.", e);
            case 429 -> new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Ai atins limita de cereri Gemini. Așteaptă un minut și încearcă din nou.", e);
            default -> new ApiException(HttpStatus.BAD_GATEWAY,
                    "Gemini nu este disponibil momentan (" + status + "). Încearcă din nou.", e);
        };
    }

    static String stripCodeFences(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                t = t.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return t;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String shorten(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Cererea a fost întreruptă.");
        }
    }

    // ---- DTO-uri pentru raspunsul generateContent ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateContentResponse(List<Candidate> candidates, PromptFeedback promptFeedback) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text, Boolean thought) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PromptFeedback(String blockReason) {
    }
}
