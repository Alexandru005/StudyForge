CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Tabela pentru Planurile de Învățare
CREATE TABLE learning_plan (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    pdf_file_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela pentru Quiz-uri
CREATE TABLE quiz (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    learning_plan_id UUID NOT NULL,
    learning_stage VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_learning_plan
      FOREIGN KEY (learning_plan_id)
          REFERENCES learning_plan(id)
          ON DELETE CASCADE
);

-- 3. Tabela pentru Flashcards
CREATE TABLE flashcard (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quiz_id UUID NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    is_mastered BOOLEAN DEFAULT FALSE, -- Opțional: te ajută să filtrezi ce ai învățat deja
    CONSTRAINT fk_quiz
       FOREIGN KEY (quiz_id)
           REFERENCES quiz(id)
           ON DELETE CASCADE
);