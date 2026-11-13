package com.studyforge.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class FileSupportTest {

    @Test
    void slugRemovesRomanianDiacritics() {
        assertThat(FileSupport.slug("Analiză matematică — Ședința ȚĂRII")).isEqualTo("analiza-matematica-sedinta-tarii");
        assertThat(FileSupport.slug("  ")).isEqualTo("document");
        assertThat(FileSupport.slug("???")).isEqualTo("document");
    }

    @Test
    void detectsPdfMagic() {
        assertThat(FileSupport.isPdf("%PDF-1.7 ...".getBytes(StandardCharsets.US_ASCII))).isTrue();
        assertThat(FileSupport.isPdf("\n\n%PDF-1.4".getBytes(StandardCharsets.US_ASCII))).isTrue();
        assertThat(FileSupport.isPdf("PK zip".getBytes(StandardCharsets.US_ASCII))).isFalse();
        assertThat(FileSupport.isPdf(new byte[0])).isFalse();
    }
}
