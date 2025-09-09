CREATE TABLE IF NOT EXISTS database_info (
    id SERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO database_info (version) VALUES ('1.0.0');