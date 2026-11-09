-- Employee Task Assignment System - MySQL schema
-- The application creates these tables automatically (spring.jpa.hibernate.ddl-auto=update).
-- Use this script only if you prefer to create the database by hand:
--   mysql -u root -p < database/schema.sql

CREATE DATABASE IF NOT EXISTS task_assignment_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE task_assignment_db;

CREATE TABLE IF NOT EXISTS employees (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    department   VARCHAR(100) NOT NULL,
    designation  VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tasks (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    title        VARCHAR(150)  NOT NULL,
    description  VARCHAR(1000) NULL,
    priority     VARCHAR(20)   NOT NULL,            -- LOW | MEDIUM | HIGH
    deadline     DATE          NOT NULL,
    status       VARCHAR(20)   NOT NULL,            -- PENDING | IN_PROGRESS | COMPLETED
    assigned_at  DATETIME(6)   NULL,
    created_at   DATETIME(6)   NOT NULL,
    employee_id  BIGINT        NULL,                -- NULL = unassigned
    PRIMARY KEY (id),
    CONSTRAINT fk_tasks_employee FOREIGN KEY (employee_id)
        REFERENCES employees (id) ON DELETE SET NULL,
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_tasks_status   CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    INDEX idx_tasks_employee_status (employee_id, status)
) ENGINE=InnoDB;
