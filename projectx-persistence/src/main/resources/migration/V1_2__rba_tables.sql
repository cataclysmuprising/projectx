-- =========================================================
-- MJR RBAC BASE TABLES
-- Compatible with:
-- Spring Boot 4 / JPA / QueryDSL / PostgreSQL
-- =========================================================

SET TIME ZONE 'Asia/Yangon';
SET search_path TO projectx;

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
    access_level    VARCHAR(20)  NOT NULL, -- READ, WRITE, SENSITIVE
    url             VARCHAR(250) NOT NULL,
    description     VARCHAR(200) NOT NULL,

    created_date    TIMESTAMP NOT NULL,
    created_by      BIGINT    NOT NULL,
    updated_date    TIMESTAMP NOT NULL,
    updated_by      BIGINT    NOT NULL,

    CONSTRAINT uq_mjr_action_app_url
        UNIQUE (app_name, url),

    CONSTRAINT uq_mjr_action_app_action
        UNIQUE (app_name, action_name),

    CONSTRAINT uq_mjr_action_page_action
        UNIQUE (page, action_name)
);

COMMENT ON COLUMN mjr_action.action_type IS
'MAIN = primary page access, SUB = business operation within the module';

COMMENT ON COLUMN mjr_action.access_level IS
'READ = non-mutating visibility, WRITE = ordinary state change, SENSITIVE = privileged or high-risk action';

CREATE INDEX idx_mjr_action_app_name   ON mjr_action(app_name);
CREATE INDEX idx_mjr_action_page       ON mjr_action(page);
CREATE INDEX idx_mjr_action_action     ON mjr_action(action_name);
CREATE INDEX idx_mjr_action_access     ON mjr_action(access_level);

-- =========================================================
-- ACTION ROUTE REGISTRY
-- =========================================================
CREATE TABLE mjr_action_route (
    id              BIGSERIAL PRIMARY KEY,

    app_name        VARCHAR(30)  NOT NULL,
    route_pattern   VARCHAR(250) NOT NULL,
    http_method     VARCHAR(16)  NOT NULL DEFAULT 'ANY',
    route_kind      VARCHAR(20)  NOT NULL,
    priority        INTEGER      NOT NULL DEFAULT 100,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    description     VARCHAR(200) NOT NULL,

    created_date    TIMESTAMP NOT NULL,
    created_by      BIGINT    NOT NULL,
    updated_date    TIMESTAMP NOT NULL,
    updated_by      BIGINT    NOT NULL,

    CONSTRAINT uq_mjr_action_route_app_method_pattern
        UNIQUE (app_name, http_method, route_pattern)
);

COMMENT ON COLUMN mjr_action_route.route_kind IS
'PRIMARY = main action route, PAGE_SUPPORT = helper/support route, SHARED_LOOKUP = shared lookup route';

CREATE INDEX idx_mjr_action_route_lookup
    ON mjr_action_route(app_name, active, priority DESC, id);

CREATE INDEX idx_mjr_action_route_kind
    ON mjr_action_route(route_kind);

CREATE TABLE mjr_action_route_x_action (
    id              BIGSERIAL PRIMARY KEY,

    route_id        BIGINT NOT NULL,
    action_id       BIGINT NOT NULL,

    created_date    TIMESTAMP NOT NULL,
    created_by      BIGINT    NOT NULL,
    updated_date    TIMESTAMP NOT NULL,
    updated_by      BIGINT    NOT NULL,

    CONSTRAINT uq_mjr_action_route_target
        UNIQUE (route_id, action_id),

    CONSTRAINT fk_mjr_action_route_target_route
        FOREIGN KEY (route_id)
        REFERENCES mjr_action_route(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_mjr_action_route_target_action
        FOREIGN KEY (action_id)
        REFERENCES mjr_action(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_mjr_action_route_target_route
    ON mjr_action_route_x_action(route_id);

CREATE INDEX idx_mjr_action_route_target_action
    ON mjr_action_route_x_action(action_id);

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

CREATE TABLE mjr_admin_login_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_id  BIGINT NOT NULL,
    ip_address varchar(100),
    os varchar(100),
    client_agent varchar(200),
    login_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_date    TIMESTAMP NOT NULL,
    created_by      BIGINT NOT NULL,
    updated_date    TIMESTAMP NOT NULL,
    updated_by      BIGINT NOT NULL,

    CONSTRAINT fk_admin_login_history_admin
    FOREIGN KEY (admin_id)
    REFERENCES mjr_admin(id)
    ON DELETE CASCADE
);
COMMENT ON COLUMN "mjr_admin_login_history"."os" IS 'Operating System';

-- =========================================================
-- ACTUATOR AUDIT TRAIL
-- =========================================================
CREATE TABLE mjr_actuator_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    admin_id BIGINT NULL,
    action_type VARCHAR(50) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    query_string VARCHAR(1000),
    client_ip VARCHAR(100),
    user_agent VARCHAR(500),
    status_code INTEGER,
    detail_json TEXT,

    created_date TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_date TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,

    CONSTRAINT fk_mjr_actuator_audit_admin
        FOREIGN KEY (admin_id)
        REFERENCES mjr_admin(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_mjr_actuator_audit_admin_id ON mjr_actuator_audit(admin_id);
CREATE INDEX idx_mjr_actuator_audit_action_type ON mjr_actuator_audit(action_type);
CREATE INDEX idx_mjr_actuator_audit_endpoint ON mjr_actuator_audit(endpoint);
CREATE INDEX idx_mjr_actuator_audit_created_date ON mjr_actuator_audit(created_date DESC);
