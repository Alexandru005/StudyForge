package com.studyforge.backend.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.web.multipart.MultipartFile;

/**
 * Utilitare pentru fisierele incarcate.
 */
public final class FileSupport {

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);

    private FileSupport() {
    }

    /** Citeste fisierul incarcat si verifica faptul ca e un PDF. */
    public static byte[] readPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Nu ai încărcat niciun fișier.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("Fișierul nu a putut fi citit.");
        }
        if (!isPdf(bytes)) {
            throw ApiException.badRequest("Fișierul încărcat nu este un PDF.");
        }
        return bytes;
    }

    public static boolean isPdf(byte[] bytes) {
        if (bytes == null || bytes.length < PDF_MAGIC.length) {
            return false;
        }
        // Unele PDF-uri au cativa octeti inainte de "%PDF"; cautam in primii 1024.
        int limit = Math.min(bytes.length - PDF_MAGIC.length, 1024);
        for (int i = 0; i <= limit; i++) {
            boolean match = true;
            for (int j = 0; j < PDF_MAGIC.length; j++) {
                if (bytes[i + j] != PDF_MAGIC[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    public static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponibil", e);
        }
    }

    /** "Analiză matematică I" -> "analiza-matematica-i" (pentru nume de fisiere). */
    public static String slug(String text) {
        if (text == null || text.isBlank()) {
            return "document";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalized.isEmpty()) {
            return "document";
        }
        return normalized.length() > 60 ? normalized.substring(0, 60).replaceAll("-+$", "") : normalized;
    }

    public static String originalName(MultipartFile file, String fallback) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return fallback;
        }
        // Unele browsere trimit calea completa
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return truncate(name, 255);
    }

    public static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
