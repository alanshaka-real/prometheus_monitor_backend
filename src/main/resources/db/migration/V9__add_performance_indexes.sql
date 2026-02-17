-- V9: Add performance indexes for frequently queried columns

CREATE INDEX idx_alert_rule_status_severity ON prom_alert_rule(status, severity);
CREATE INDEX idx_distribute_task_detail_task_status ON prom_distribute_task_detail(task_id, status);
CREATE INDEX idx_alert_history_severity_starts ON prom_alert_history(severity, starts_at);
CREATE INDEX idx_query_history_executed_at ON prom_query_history(executed_at);
