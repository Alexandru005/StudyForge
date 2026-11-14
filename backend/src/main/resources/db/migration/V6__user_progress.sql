-- Progresul complet din pagina "Progres" (pe zile si pe materii), ca sa nu se piarda cand schimbi
-- browserul / adresa sau se sterg datele din browser. Un rand per utilizator; data = JSON-ul din frontend.
CREATE TABLE user_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    data TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_progress_user UNIQUE (user_id)
);
