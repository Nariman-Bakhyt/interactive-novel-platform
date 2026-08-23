CREATE INDEX idx_novel_publication_date ON novel (publication_date DESC);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_novel_title_trgm ON novel USING gin (title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_user_username_trgm ON app_user USING gin (username gin_trgm_ops);