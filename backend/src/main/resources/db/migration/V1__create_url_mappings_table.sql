CREATE TABLE url_mappings (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    -- ascii_bin makes the column CASE SENSITIVE: "aB3" and "Ab3" are different codes.
    short_code   VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    CONSTRAINT pk_url_mappings PRIMARY KEY (id),
    CONSTRAINT uk_url_mappings_short_code UNIQUE (short_code)
);
