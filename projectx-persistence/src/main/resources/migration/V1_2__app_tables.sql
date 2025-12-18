-- =========================================================
-- MJR RBAC BASE TABLES
-- Compatible with:
-- Spring Boot 4 / JPA / QueryDSL / PostgreSQL
-- =========================================================

SET TIME ZONE 'Asia/Rangoon';

-- =========================================================
-- ACTIONS (URL / PAGE / OPERATION DEFINITIONS)
-- =========================================================
CREATE TABLE mjr_action (
    id              BIGSERIAL PRIMARY KEY,

    app_name        VARCHAR(30)  NOT NULL,
    page            VARCHAR(50)  NOT NULL,
    action_name     VARCHAR(50)  NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    action_type     VARCHAR(20)  NOT NULL, -- MAIN, SUB
    url             VARCHAR(250) NOT NULL,
    description VARCHAR(200) NOT NULL,

    created_date      TIMESTAMP NOT NULL,
    created_by      BIGINT NOT NULL,
    updated_date      TIMESTAMP NOT NULL,
    updated_by      BIGINT NOT NULL,

    CONSTRAINT uq_mjr_action_app_url
        UNIQUE (app_name, url),

    CONSTRAINT uq_mjr_action_app_action
        UNIQUE (app_name, action_name),

    CONSTRAINT uq_mjr_action_page_action
        UNIQUE (page, action_name)
);

COMMENT ON COLUMN mjr_action.action_type IS
'0 = main page action, 1 = sub-action within page';

CREATE INDEX idx_mjr_action_app_name   ON mjr_action(app_name);
CREATE INDEX idx_mjr_action_page       ON mjr_action(page);
CREATE INDEX idx_mjr_action_action     ON mjr_action(action_name);

-- =========================================================
-- ROLES
-- =========================================================
CREATE TABLE mjr_role (
    id              BIGSERIAL PRIMARY KEY,

    app_name        VARCHAR(30) NOT NULL,
    name            VARCHAR(20) NOT NULL,
    type            VARCHAR(20) NOT NULL DEFAULT 'BUILT_IN',
    description     VARCHAR(200),

    created_date      TIMESTAMP NOT NULL,
    created_by      BIGINT NOT NULL,
    updated_date      TIMESTAMP NOT NULL,
    updated_by      BIGINT NOT NULL,

    CONSTRAINT uq_mjr_role_app_name
        UNIQUE (app_name, name)
);

CREATE INDEX idx_mjr_role_app_name ON mjr_role(app_name);
CREATE INDEX idx_mjr_role_name     ON mjr_role(name);

-- =========================================================
-- ROLE ↔ ACTION MAPPING
-- =========================================================
CREATE TABLE mjr_role_x_action (
    id              BIGSERIAL PRIMARY KEY,

    role_id         BIGINT NOT NULL,
    action_id       BIGINT NOT NULL,

    created_date      TIMESTAMP NOT NULL,
    created_by      BIGINT NOT NULL,
    updated_date      TIMESTAMP NOT NULL,
    updated_by      BIGINT NOT NULL,

    CONSTRAINT uq_mjr_role_action
        UNIQUE (role_id, action_id),

    CONSTRAINT fk_mjr_role_action_role
        FOREIGN KEY (role_id)
        REFERENCES mjr_role(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_mjr_role_action_action
        FOREIGN KEY (action_id)
        REFERENCES mjr_action(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_mjr_role_x_action_role   ON mjr_role_x_action(role_id);
CREATE INDEX idx_mjr_role_x_action_action ON mjr_role_x_action(action_id);

-- =========================================================
-- ADMINS
-- =========================================================
CREATE TABLE mjr_admin (
    id              BIGSERIAL PRIMARY KEY,

    name            VARCHAR(50) NOT NULL,
    login_id        VARCHAR(50) NOT NULL,
    password        VARCHAR(200) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED

    created_date      TIMESTAMP NOT NULL,
    created_by      BIGINT NOT NULL,
    updated_date      TIMESTAMP NOT NULL,
    updated_by      BIGINT NOT NULL,

    CONSTRAINT uq_mjr_admin_login
        UNIQUE (login_id)
);

CREATE INDEX idx_mjr_admin_status ON mjr_admin(status);

-- =========================================================
-- ADMIN ↔ ROLE MAPPING
-- =========================================================
CREATE TABLE mjr_admin_x_role (
    id              BIGSERIAL PRIMARY KEY,

    admin_id        BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,

    created_date      TIMESTAMP NOT NULL,
    created_by      BIGINT NOT NULL,
    updated_date      TIMESTAMP NOT NULL,
    updated_by      BIGINT NOT NULL,

    CONSTRAINT uq_mjr_admin_role
        UNIQUE (admin_id, role_id),

    CONSTRAINT fk_mjr_admin_role_admin
        FOREIGN KEY (admin_id)
        REFERENCES mjr_admin(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_mjr_admin_role_role
        FOREIGN KEY (role_id)
        REFERENCES mjr_role(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_mjr_admin_x_role_admin ON mjr_admin_x_role(admin_id);
CREATE INDEX idx_mjr_admin_x_role_role  ON mjr_admin_x_role(role_id);
