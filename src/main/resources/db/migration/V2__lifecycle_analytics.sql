ALTER TABLE links ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE links ADD COLUMN disabled_at TIMESTAMP WITH TIME ZONE;
CREATE TABLE daily_clicks (
    code VARCHAR(32) NOT NULL REFERENCES links(code),
    click_date DATE NOT NULL,
    clicks BIGINT NOT NULL CHECK (clicks >= 0),
    PRIMARY KEY (code, click_date)
);
