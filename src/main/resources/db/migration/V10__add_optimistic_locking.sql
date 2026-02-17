-- V10: Add optimistic locking version column to key tables
-- Note: MySQL < 10.0 does not support ADD COLUMN IF NOT EXISTS
ALTER TABLE prom_dashboard ADD COLUMN version INT DEFAULT 0;
ALTER TABLE prom_alert_rule ADD COLUMN version INT DEFAULT 0;
ALTER TABLE prom_instance ADD COLUMN opt_version INT DEFAULT 0;
ALTER TABLE prom_exporter ADD COLUMN version INT DEFAULT 0;

-- Add silenced field to alert history for silence rule matching
ALTER TABLE prom_alert_history ADD COLUMN silenced TINYINT(1) DEFAULT 0;
