package com.studyforge.backend.pdf;

/** Chei custom scrise in metadatele PDF-urilor generate de StudyForge. */
public final class PdfMetadata {

    /** ID-ul planului de invatare; il citim cand utilizatorul reincarca PDF-ul planului pe pagina 3. */
    public static final String PLAN_ID = "StudyForgePlanId";

    /** Tipul documentului: "plan" sau "flashcards". */
    public static final String DOC_TYPE = "StudyForgeType";

    private PdfMetadata() {
    }
}
