-- Progresul pe zile, trimis de frontend din pagina "Progres" (localStorage), adunat pe toate materiile.
-- Din el se calculeaza clasamentul pe saptamani (luni - duminica).
CREATE TABLE study_day (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    study_date DATE NOT NULL,
    correct_count INTEGER NOT NULL DEFAULT 0,
    wrong_count INTEGER NOT NULL DEFAULT 0,
    active_seconds INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_study_day_user_date UNIQUE (user_id, study_date),
    CONSTRAINT ck_study_day_counts CHECK (correct_count >= 0 AND wrong_count >= 0
        AND active_seconds >= 0 AND active_seconds <= 86400)
);

-- Prietenii: o cerere (PENDING) devine prietenie (ACCEPTED) cand o accepta destinatarul.
-- Un singur rand per pereche de utilizatori, indiferent cine a trimis cererea.
CREATE TABLE friendship (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requester_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    addressee_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    status VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMPTZ,
    CONSTRAINT ck_friendship_status CHECK (status IN ('PENDING', 'ACCEPTED')),
    CONSTRAINT ck_friendship_not_self CHECK (requester_id <> addressee_id)
);
CREATE UNIQUE INDEX uq_friendship_pair
    ON friendship (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
CREATE INDEX idx_friendship_requester ON friendship (requester_id);
CREATE INDEX idx_friendship_addressee ON friendship (addressee_id);

-- Like / dislike la procentajul unui prieten, pentru o anumita saptamana din clasament.
-- Cel mult o reactie per (de la, catre, saptamana); week_start = lunea saptamanii.
CREATE TABLE friend_reaction (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    to_user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    type VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_friend_reaction_type CHECK (type IN ('LIKE', 'DISLIKE')),
    CONSTRAINT ck_friend_reaction_not_self CHECK (from_user_id <> to_user_id),
    CONSTRAINT ck_friend_reaction_monday CHECK (EXTRACT(ISODOW FROM week_start) = 1),
    CONSTRAINT uq_friend_reaction UNIQUE (from_user_id, to_user_id, week_start)
);
CREATE INDEX idx_friend_reaction_to_week ON friend_reaction (to_user_id, week_start);
