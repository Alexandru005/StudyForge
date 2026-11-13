package com.studyforge.backend.pdf;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.studyforge.backend.common.ApiException;

/**
 * Extrage textul si metadatele StudyForge dintr-un PDF incarcat.
 */
@Component
public class PdfTextExtractor {

    /** Un PDF cu mai putin text decat atat e probabil scanat (imagini). */
    private static final int MIN_TEXT_CHARS = 40;

    public Extracted extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = normalize(stripper.getText(document));
            if (text.replaceAll("\\s+", "").length() < MIN_TEXT_CHARS) {
                throw ApiException.unprocessable(
                        "PDF-ul nu conține text selectabil (pare scanat). Încarcă un PDF cu text.");
            }
            PDDocumentInformation info = document.getDocumentInformation();
            String planId = info != null ? info.getCustomMetadataValue(PdfMetadata.PLAN_ID) : null;
            String docType = info != null ? info.getCustomMetadataValue(PdfMetadata.DOC_TYPE) : null;
            return new Extracted(text, document.getNumberOfPages(), planId, docType);
        } catch (InvalidPasswordException e) {
            throw ApiException.unprocessable("PDF-ul este protejat cu parolă.");
        } catch (IOException e) {
            throw ApiException.unprocessable("PDF-ul nu a putut fi citit (fișier corupt?).");
        }
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace(' ', ' ')
                // Unele PDF-uri romanesti folosesc vechile ş/ţ cu sedila
                .replace('ş', 'ș').replace('Ş', 'Ș')
                .replace('ţ', 'ț').replace('Ţ', 'Ț')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * @param text      textul PDF-ului
     * @param pageCount numarul de pagini
     * @param planId    ID-ul planului StudyForge din metadate (null daca PDF-ul nu e generat de noi)
     * @param docType   "plan" / "flashcards" pentru PDF-urile StudyForge, altfel null
     */
    public record Extracted(String text, int pageCount, String planId, String docType) {
    }
}
