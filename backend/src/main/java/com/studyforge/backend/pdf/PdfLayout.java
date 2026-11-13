package com.studyforge.backend.pdf;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/**
 * Mic "motor" de layout peste PDFBox: titluri, paragrafe cu word-wrap, liste, casete, carduri,
 * paginare automata si subsol cu numarul paginii. Foloseste DejaVu Sans (inclus in resurse),
 * deci diacriticele romanesti (ă â î ș ț) apar corect.
 */
final class PdfLayout implements AutoCloseable {

    static final Color PRIMARY = new Color(30, 58, 95);
    static final Color ACCENT = new Color(214, 96, 18);
    static final Color TEXT = new Color(33, 37, 41);
    static final Color MUTED = new Color(108, 117, 125);
    static final Color LIGHT_BG = new Color(243, 246, 251);
    static final Color ACCENT_BG = new Color(253, 240, 228);
    static final Color BORDER = new Color(210, 216, 226);

    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 50f;
    private static final float TOP = PAGE_SIZE.getHeight() - MARGIN;
    private static final float BOTTOM = 60f;
    private static final float CONTENT_WIDTH = PAGE_SIZE.getWidth() - 2 * MARGIN;
    private static final float LINE_FACTOR = 1.38f;

    private static byte[] regularFontBytes;
    private static byte[] boldFontBytes;

    private final PDDocument document;
    private final PDFont regular;
    private final PDFont bold;
    private final Map<PDFont, Map<Integer, Boolean>> glyphCache = new HashMap<>();
    private final String footerText;

    private PDPageContentStream content;
    private float y;

    PdfLayout(String footerText) throws IOException {
        this.document = new PDDocument();
        this.regular = PDType0Font.load(document, new ByteArrayInputStream(fontBytes(false)));
        this.bold = PDType0Font.load(document, new ByteArrayInputStream(fontBytes(true)));
        this.footerText = footerText;
        newPage();
    }

    // ------------------------------------------------------------------ blocuri de continut

    void title(String text) throws IOException {
        writeText(text, bold, 20f, PRIMARY, 0f);
        y -= 4f;
    }

    void subtitle(String text) throws IOException {
        writeText(text, regular, 11f, MUTED, 0f);
        y -= 6f;
        rule(PRIMARY, 1.5f);
        y -= 10f;
    }

    void section(String text) throws IOException {
        ensureSpace(70f);
        y -= 8f;
        writeText(text, bold, 14f, PRIMARY, 0f);
        y -= 4f;
    }

    void label(String text) throws IOException {
        ensureSpace(40f);
        y -= 2f;
        writeText(text, bold, 10f, MUTED, 0f);
        y -= 1f;
    }

    void paragraph(String text) throws IOException {
        writeText(text, regular, 10.5f, TEXT, 0f);
        y -= 4f;
    }

    void emphasis(String text) throws IOException {
        writeText(text, bold, 10.5f, TEXT, 0f);
        y -= 3f;
    }

    void bullets(List<String> items) throws IOException {
        bullets(items, TEXT);
    }

    void bullets(List<String> items, Color color) throws IOException {
        float size = 10.5f;
        float leading = size * LINE_FACTOR;
        float indent = 14f;
        for (String item : items) {
            if (item == null || item.isBlank()) {
                continue;
            }
            List<String> lines = wrap(item, regular, size, CONTENT_WIDTH - indent);
            for (int i = 0; i < lines.size(); i++) {
                ensureSpace(leading);
                float baseline = y - size;
                if (i == 0) {
                    drawText("•", regular, size, ACCENT, MARGIN + 3f, baseline);
                }
                drawText(lines.get(i), regular, size, color, MARGIN + indent, baseline);
                y -= leading;
            }
            y -= 1.5f;
        }
        y -= 3f;
    }

    /** Bara colorata pentru inceputul unei saptamani: "Săptămâna 3" ... "17.11 – 23.11.2026". */
    void bandHeader(String left, String right) throws IOException {
        float height = 24f;
        ensureSpace(height + 80f); // nu lasam bara singura la final de pagina
        y -= 6f;
        content.setNonStrokingColor(PRIMARY);
        content.addRect(MARGIN, y - height, CONTENT_WIDTH, height);
        content.fill();
        float baseline = y - 16f;
        drawText(fit(left, bold, 12f, CONTENT_WIDTH * 0.6f), bold, 12f, Color.WHITE, MARGIN + 10f, baseline);
        if (right != null && !right.isBlank()) {
            String r = fit(right, regular, 10f, CONTENT_WIDTH * 0.38f);
            float w = width(r, regular, 10f);
            drawText(r, regular, 10f, Color.WHITE, MARGIN + CONTENT_WIDTH - 10f - w, baseline);
        }
        y -= height + 8f;
    }

    /** Caseta evidentiata (ex. evaluarile din saptamana). */
    void callout(String heading, String text) throws IOException {
        float size = 10f;
        float leading = size * LINE_FACTOR;
        float padding = 8f;
        float barWidth = 4f;
        float innerWidth = CONTENT_WIDTH - 2 * padding - barWidth;
        List<String> headingLines = wrap(heading, bold, size, innerWidth);
        List<String> lines = wrap(text, regular, size, innerWidth);
        float height = 2 * padding + (headingLines.size() + lines.size()) * leading;
        ensureSpace(height + 6f);
        y -= 2f;
        float top = y;
        content.setNonStrokingColor(ACCENT_BG);
        content.addRect(MARGIN, top - height, CONTENT_WIDTH, height);
        content.fill();
        content.setNonStrokingColor(ACCENT);
        content.addRect(MARGIN, top - height, barWidth, height);
        content.fill();
        float cursor = top - padding;
        for (String line : headingLines) {
            drawText(line, bold, size, ACCENT, MARGIN + barWidth + padding, cursor - size);
            cursor -= leading;
        }
        for (String line : lines) {
            drawText(line, regular, size, TEXT, MARGIN + barWidth + padding, cursor - size);
            cursor -= leading;
        }
        y = top - height - 8f;
    }

    /** Un flashcard: eticheta, intrebarea (bold) si raspunsul, intr-o caseta. */
    void card(int number, String question, String answer) throws IOException {
        float qSize = 11f;
        float aSize = 10.5f;
        float labelSize = 8f;
        float padding = 10f;
        float innerWidth = CONTENT_WIDTH - 2 * padding;
        List<String> qLines = wrap(question, bold, qSize, innerWidth);
        List<String> aLines = wrap(answer, regular, aSize, innerWidth);
        float labelLeading = labelSize * 1.6f;
        float height = 2 * padding
                + labelLeading + qLines.size() * qSize * LINE_FACTOR
                + 10f
                + labelLeading + aLines.size() * aSize * LINE_FACTOR;

        if (height > TOP - BOTTOM - 20f) {
            // Card neobisnuit de lung: il scriem fara caseta, ca sa poata continua pe pagina urmatoare.
            label("ÎNTREBAREA " + number);
            emphasis(question);
            label("RĂSPUNS");
            paragraph(answer);
            rule(BORDER, 0.8f);
            y -= 8f;
            return;
        }

        ensureSpace(height + 10f);
        float top = y;
        content.setNonStrokingColor(LIGHT_BG);
        content.addRect(MARGIN, top - height, CONTENT_WIDTH, height);
        content.fill();
        content.setStrokingColor(BORDER);
        content.setLineWidth(0.8f);
        content.addRect(MARGIN, top - height, CONTENT_WIDTH, height);
        content.stroke();

        float x = MARGIN + padding;
        float cursor = top - padding;
        drawText("ÎNTREBAREA " + number, bold, labelSize, ACCENT, x, cursor - labelSize);
        cursor -= labelLeading;
        for (String line : qLines) {
            drawText(line, bold, qSize, TEXT, x, cursor - qSize);
            cursor -= qSize * LINE_FACTOR;
        }
        cursor -= 4f;
        content.setStrokingColor(BORDER);
        content.setLineWidth(0.6f);
        content.moveTo(x, cursor);
        content.lineTo(MARGIN + CONTENT_WIDTH - padding, cursor);
        content.stroke();
        cursor -= 6f;
        drawText("RĂSPUNS", bold, labelSize, PRIMARY, x, cursor - labelSize);
        cursor -= labelLeading;
        for (String line : aLines) {
            drawText(line, regular, aSize, TEXT, x, cursor - aSize);
            cursor -= aSize * LINE_FACTOR;
        }
        y = top - height - 10f;
    }

    void spacer(float height) {
        y -= height;
    }

    void rule(Color color, float lineWidth) throws IOException {
        ensureSpace(4f);
        content.setStrokingColor(color);
        content.setLineWidth(lineWidth);
        content.moveTo(MARGIN, y);
        content.lineTo(MARGIN + CONTENT_WIDTH, y);
        content.stroke();
        y -= 2f;
    }

    // ------------------------------------------------------------------ finalizare

    /** Adauga subsolul pe fiecare pagina, metadatele, si returneaza PDF-ul ca bytes. */
    byte[] finish(String title, Map<String, String> customMetadata) throws IOException {
        content.close();
        content = null;

        int total = document.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream footer = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                footer.setStrokingColor(BORDER);
                footer.setLineWidth(0.6f);
                footer.moveTo(MARGIN, 44f);
                footer.lineTo(MARGIN + CONTENT_WIDTH, 44f);
                footer.stroke();
                String left = fit(sanitize(footerText, regular), regular, 8f, CONTENT_WIDTH * 0.7f);
                String right = "Pagina " + (i + 1) + " din " + total;
                writeRaw(footer, left, regular, 8f, MUTED, MARGIN, 32f);
                writeRaw(footer, right, regular, 8f, MUTED,
                        MARGIN + CONTENT_WIDTH - width(right, regular, 8f), 32f);
            }
        }

        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle(title);
        info.setAuthor("StudyForge");
        info.setCreator("StudyForge");
        info.setProducer("StudyForge (Apache PDFBox)");
        info.setCreationDate(Calendar.getInstance());
        if (customMetadata != null) {
            customMetadata.forEach((key, value) -> {
                if (value != null) {
                    info.setCustomMetadataValue(key, value);
                }
            });
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        return out.toByteArray();
    }

    @Override
    public void close() throws IOException {
        if (content != null) {
            content.close();
            content = null;
        }
        document.close();
    }

    // ------------------------------------------------------------------ interne

    private void newPage() throws IOException {
        if (content != null) {
            content.close();
        }
        PDPage page = new PDPage(PAGE_SIZE);
        document.addPage(page);
        content = new PDPageContentStream(document, page);
        y = TOP;
    }

    private void ensureSpace(float needed) throws IOException {
        if (y - needed < BOTTOM) {
            newPage();
        }
    }

    private void writeText(String text, PDFont font, float size, Color color, float indent) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        float leading = size * LINE_FACTOR;
        for (String line : wrap(text, font, size, CONTENT_WIDTH - indent)) {
            ensureSpace(leading);
            drawText(line, font, size, color, MARGIN + indent, y - size);
            y -= leading;
        }
    }

    /** Textul trebuie sa fie deja trecut prin wrap()/sanitize(). */
    private void drawText(String text, PDFont font, float size, Color color, float x, float baseline)
            throws IOException {
        writeRaw(content, text, font, size, color, x, baseline);
    }

    private static void writeRaw(PDPageContentStream stream, String text, PDFont font, float size, Color color,
            float x, float baseline) throws IOException {
        if (text == null || text.isEmpty()) {
            return;
        }
        stream.beginText();
        stream.setFont(font, size);
        stream.setNonStrokingColor(color);
        stream.newLineAtOffset(x, baseline);
        stream.showText(text);
        stream.endText();
    }

    /** Word-wrap: imparte textul in linii care incap in maxWidth. Respecta \n din text. */
    List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        String clean = sanitize(text, font);
        for (String paragraph : clean.split("\n", -1)) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) {
                if (!lines.isEmpty()) {
                    lines.add("");
                }
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : trimmed.split(" +")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (width(candidate, font, size) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                // Cuvant mai lung decat linia (ex. URL): il taiem pe caractere.
                String rest = word;
                while (width(rest, font, size) > maxWidth) {
                    int cut = rest.length() - 1;
                    while (cut > 1 && width(rest.substring(0, cut), font, size) > maxWidth) {
                        cut--;
                    }
                    lines.add(rest.substring(0, cut));
                    rest = rest.substring(cut);
                }
                current.append(rest);
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    private String fit(String text, PDFont font, float size, float maxWidth) throws IOException {
        String clean = sanitize(text, font);
        if (width(clean, font, size) <= maxWidth) {
            return clean;
        }
        String ellipsis = "…";
        String result = clean;
        while (result.length() > 1 && width(result + ellipsis, font, size) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result.strip() + ellipsis;
    }

    private static float width(String text, PDFont font, float size) throws IOException {
        return font.getStringWidth(text) / 1000f * size;
    }

    /**
     * Inlocuieste caracterele pe care fontul nu le are (ex. emoji) cu '?', ca showText sa nu arunce exceptie.
     * Tab-urile devin spatii; \n se pastreaza (e tratat de wrap()).
     */
    String sanitize(String text, PDFont font) {
        Map<Integer, Boolean> cache = glyphCache.computeIfAbsent(font, f -> new HashMap<>());
        StringBuilder sb = new StringBuilder(text.length());
        text.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ')
                .replace('ş', 'ș').replace('Ş', 'Ș').replace('ţ', 'ț').replace('Ţ', 'Ț')
                .codePoints()
                .forEach(cp -> {
                    if (cp == '\n') {
                        sb.append('\n');
                    } else if (Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT) {
                        sb.append(' ');
                    } else if (cache.computeIfAbsent(cp, c -> canEncode(font, c))) {
                        sb.appendCodePoint(cp);
                    } else {
                        sb.append('?');
                    }
                });
        return sb.toString();
    }

    private static boolean canEncode(PDFont font, int codePoint) {
        try {
            font.encode(new String(Character.toChars(codePoint)));
            return true;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    private static synchronized byte[] fontBytes(boolean boldFont) {
        if (regularFontBytes == null) {
            regularFontBytes = readResource("/fonts/DejaVuSans.ttf");
            boldFontBytes = readResource("/fonts/DejaVuSans-Bold.ttf");
        }
        return boldFont ? boldFontBytes : regularFontBytes;
    }

    private static byte[] readResource(String path) {
        try (InputStream in = PdfLayout.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Lipseste fontul " + path + " din resurse");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
