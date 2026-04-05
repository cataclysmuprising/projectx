SET search_path TO projectx;

-- Temporarily disable triggers to allow deterministic cleanup
SET session_replication_role = replica;