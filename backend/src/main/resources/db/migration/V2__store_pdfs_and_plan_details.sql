-- PDF-urile (planuri + flashcard-uri) se salveaza direct in Postgres, ca bytea.
CREATE TABLE stored_pdf (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_name VARCHAR(255) NOT NULL,
    content BYTEA NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_stored_pdf_sha256 ON stored_pdf (sha256);

-- Planul de invatare: detalii + structura pe saptamani (JSON) + PDF-ul generat
ALTER TABLE learning_plan DROP COLUMN pdf_file_url;
ALTER TABLE learning_plan ADD COLUMN subject VARCHAR(255);
ALTER TABLE learning_plan ADD COLUMN syllabus_file_name VARCHAR(255);
ALTER TABLE learning_plan ADD COLUMN start_date DATE;
ALTER TABLE learning_plan ADD COLUMN end_date DATE;
ALTER TABLE learning_plan ADD COLUMN week_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE learning_plan ADD COLUMN content_json TEXT;
ALTER TABLE learning_plan ADD COLUMN pdf_id UUID REFERENCES stored_pdf (id);

-- Quiz = un set de flashcard-uri pentru o saptamana.
-- learning_plan_id devine optional: daca PDF-ul incarcat nu e recunoscut ca plan StudyForge,
-- setul se genereaza oricum, doar ca nu e legat de un plan.
ALTER TABLE quiz ALTER COLUMN learning_plan_id DROP NOT NULL;
ALTER TABLE quiz ADD COLUMN week_number INTEGER;
ALTER TABLE quiz ADD COLUMN source_file_name VARCHAR(255);
ALTER TABLE quiz ADD COLUMN pdf_id UUID REFERENCES stored_pdf (id);
UPDATE quiz SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE learning_plan SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

ALTER TABLE flashcard ADD COLUMN order_index INTEGER NOT NULL DEFAULT 0;
UPDATE flashcard SET is_mastered = FALSE WHERE is_mastered IS NULL;

CREATE INDEX idx_quiz_learning_plan ON quiz (learning_plan_id);
CREATE INDEX idx_flashcard_quiz ON flashcard (quiz_id);
