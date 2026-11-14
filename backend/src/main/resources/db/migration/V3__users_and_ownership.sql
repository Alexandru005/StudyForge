-- Conturi de utilizator. "user" e cuvant rezervat in Postgres, de aceea app_user.
CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL,          -- nume si prenume, NU e unic
    email VARCHAR(255) NOT NULL,             -- salvat cu litere mici, unic
    password_hash VARCHAR(100) NOT NULL,     -- BCrypt
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_email UNIQUE (email)
);

-- Fiecare plan / set de flashcard-uri apartine unui utilizator.
-- Coloana e nullable doar pentru datele create inainte de autentificare (nu mai apar in liste).
ALTER TABLE learning_plan ADD COLUMN owner_id UUID REFERENCES app_user (id) ON DELETE CASCADE;
ALTER TABLE quiz ADD COLUMN owner_id UUID REFERENCES app_user (id) ON DELETE CASCADE;

CREATE INDEX idx_learning_plan_owner ON learning_plan (owner_id);
CREATE INDEX idx_quiz_owner ON quiz (owner_id);
