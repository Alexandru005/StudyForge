package com.studyforge.backend.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schemele JSON (formatul OpenAPI folosit de Gemini responseSchema) pentru raspunsurile asteptate.
 */
public final class GeminiSchemas {

    private GeminiSchemas() {
    }

    public static Map<String, Object> plan() {
        Map<String, Object> assessment = object(
                props(
                        "name", string("Numele evaluării, ex. \"Test 1\", \"Colocviu\", \"Examen final\""),
                        "date", string("Data evaluării în format YYYY-MM-DD sau \"\" dacă nu e cunoscută"),
                        "type", string("Tipul: test, colocviu, laborator, proiect, examen etc."),
                        "coverage", array(string("Temă/capitol care intră în evaluare"))),
                List.of("name", "date", "coverage"));

        Map<String, Object> week = object(
                props(
                        "weekNumber", integer("Numărul săptămânii, începând de la 1"),
                        "focus", string("Obiectivul principal al săptămânii, o propoziție scurtă"),
                        "topics", array(string("Temă de studiat în această săptămână")),
                        "tasks", array(string("Activitate concretă de învățare")),
                        "assessments", array(string("Numele evaluărilor care au loc în această săptămână"))),
                List.of("weekNumber", "focus", "topics", "tasks", "assessments"));

        return object(
                props(
                        "title", string("Titlul planului, ex. \"Plan de învățare – Baze de date\""),
                        "subject", string("Numele materiei"),
                        "summary", string("2-3 propoziții despre cum e organizat planul"),
                        "assessments", array(assessment),
                        "weeks", array(week),
                        "tips", array(string("Sfat scurt de învățare pentru această materie"))),
                List.of("title", "subject", "summary", "assessments", "weeks", "tips"));
    }

    public static Map<String, Object> flashcards() {
        Map<String, Object> card = object(
                props(
                        "question", string("Întrebarea"),
                        "answer", string("Răspunsul corect, concis")),
                List.of("question", "answer"));
        return object(
                props(
                        "weekLabel", string("Eticheta săptămânii, ex. \"Săptămâna 3 – Integrale\", sau \"\" dacă nu există"),
                        "flashcards", array(card)),
                List.of("weekLabel", "flashcards"));
    }

    public static Map<String, Object> hint() {
        return object(
                props("hint", string("Indiciul, maximum 2 propoziții, fără să dezvăluie răspunsul")),
                List.of("hint"));
    }

    // ---- helpers ----

    private static Map<String, Object> props(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("propertyOrdering", List.copyOf(properties.keySet()));
        return schema;
    }

    private static Map<String, Object> array(Map<String, Object> items) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "ARRAY");
        schema.put("items", items);
        return schema;
    }

    private static Map<String, Object> string(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "STRING");
        schema.put("description", description);
        return schema;
    }

    private static Map<String, Object> integer(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "INTEGER");
        schema.put("description", description);
        return schema;
    }
}
