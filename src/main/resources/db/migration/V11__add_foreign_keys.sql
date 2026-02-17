-- V11: Add foreign key constraints for association tables

-- Clean orphan records before adding constraints
DELETE FROM sys_user_role WHERE user_id NOT IN (SELECT id FROM sys_user WHERE deleted = 0);
DELETE FROM sys_user_role WHERE role_id NOT IN (SELECT id FROM sys_role WHERE deleted = 0);
DELETE FROM sys_role_permission WHERE role_id NOT IN (SELECT id FROM sys_role WHERE deleted = 0);
DELETE FROM sys_role_permission WHERE permission_id NOT IN (SELECT id FROM sys_permission);
DELETE FROM prom_workspace_dashboard WHERE workspace_id NOT IN (SELECT id FROM prom_workspace WHERE deleted = 0);
DELETE FROM prom_workspace_dashboard WHERE dashboard_id NOT IN (SELECT id FROM prom_dashboard WHERE deleted = 0);

-- sys_user_role foreign keys (CASCADE delete)
ALTER TABLE sys_user_role
    ADD CONSTRAINT fk_user_role_user_id FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_role_role_id FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE;

-- sys_role_permission foreign keys (CASCADE delete)
ALTER TABLE sys_role_permission
    ADD CONSTRAINT fk_role_perm_role_id FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_role_perm_perm_id FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE;

-- prom_workspace_dashboard foreign keys (CASCADE delete)
ALTER TABLE prom_workspace_dashboard
    ADD CONSTRAINT fk_ws_dash_workspace_id FOREIGN KEY (workspace_id) REFERENCES prom_workspace(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ws_dash_dashboard_id FOREIGN KEY (dashboard_id) REFERENCES prom_dashboard(id) ON DELETE CASCADE;
