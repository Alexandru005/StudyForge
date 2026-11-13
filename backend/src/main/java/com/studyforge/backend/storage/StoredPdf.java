package com.studyforge.backend.storage;

import java.time.Instant;

import com.studyforge.backend.common.BaseEntity;
import com.studyforge.backend.common.FileSupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Un PDF generat de aplicatie, salvat direct in Postgres (bytea).
 * Planurile si seturile de flashcard-uri tin doar o referinta LAZY catre el,
 * asa ca listarea nu incarca bytes-ii.
 */
@Entity
@Table(name = "stored_pdf")
public class StoredPdf extends BaseEntity {

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content", nullable = false, columnDefinition = "bytea")
    private byte[] content;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    protected StoredPdf() {
    }

    public StoredPdf(String fileName, byte[] content) {
        this.fileName = FileSupport.truncate(fileName, 255);
        this.content = content;
        this.sizeBytes = content.length;
        this.sha256 = FileSupport.sha256(content);
        this.createdAt = Instant.now();
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getContent() {
        return content;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
