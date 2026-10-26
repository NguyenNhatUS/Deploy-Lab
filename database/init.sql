-- Run once as a MySQL administrator. Replace the sample password before running.
-- If this account already exists, use its existing password or ALTER USER explicitly.
CREATE DATABASE IF NOT EXISTS deploy_lab
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'deploy_lab'@'localhost' IDENTIFIED BY 'change-me-local-only';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON deploy_lab.* TO 'deploy_lab'@'localhost';

-- Hibernate creates the notes table on the first application startup (lab only).
