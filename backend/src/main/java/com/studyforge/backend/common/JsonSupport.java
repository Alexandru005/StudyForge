package com.studyforge.backend.common;

import org.springframework.stereotype.Component;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Un ObjectMapper propriu (Jackson 3, cel folosit de Spring Boot 4), tolerant la campuri necunoscute.
 * Il folosim pentru raspunsurile Gemini si pentru JSON-ul planului salvat in DB.
 */
@Component
public class JsonSupport {

    private final ObjectMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public String write(Object value) {
        return mapper.writeValueAsString(value);
    }

    public <T> T read(String json, Class<T> type) {
        return mapper.readValue(json, type);
    }
}
