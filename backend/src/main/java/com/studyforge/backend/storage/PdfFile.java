package com.studyforge.backend.storage;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Un PDF gata de trimis catre browser. */
public record PdfFile(String fileName, byte[] content) {

    /**
     * @param download true = "attachment" (descarcare), false = "inline" (deschidere in browser)
     */
    public ResponseEntity<byte[]> toResponse(boolean download) {
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(content.length)
                .body(content);
    }
}
