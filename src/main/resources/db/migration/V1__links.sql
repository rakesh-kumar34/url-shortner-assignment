CREATE TABLE links (
    code VARCHAR(32) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    destination VARCHAR(2048) NOT NULL,
    title VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    total_clicks BIGINT NOT NULL DEFAULT 0 CHECK (total_clicks >= 0),
    last_clicked_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX links_created_idx ON links (created_at DESC, code);
