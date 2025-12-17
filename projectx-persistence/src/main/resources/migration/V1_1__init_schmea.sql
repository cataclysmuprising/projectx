-- =========================================================
-- V1__init_schema.sql
-- Initial schema bootstrap (DEV ONLY)
-- =========================================================

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS projectx;

-- Set default schema for this session
SET search_path TO projectx;

-- Optional: extensions you rely on
-- (only if you use them)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgstattuple;
CREATE EXTENSION IF NOT EXISTS amcheck;
