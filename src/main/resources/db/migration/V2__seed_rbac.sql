-- =============================================================
-- V2__seed_rbac.sql
-- Seed des rôles, permissions et associations role_permissions
-- Conforme à la matrice RBAC du cahier des charges §3
-- =============================================================

-- 3 rôles
INSERT INTO roles (name, description) VALUES
    ('USER',          'Utilisateur standard de la plateforme'),
    ('ADMIN',         'Administrateur système avec tous les privilèges'),
    ('SUPPORT_AGENT', 'Agent du support client');

-- 6 permissions
INSERT INTO permissions (code, description) VALUES
    ('USER_LIST_VIEW',        'Lister et rechercher tous les utilisateurs'),
    ('USER_ROLE_MANAGE',      'Modifier le rôle d''un utilisateur'),
    ('USER_STATUS_MANAGE',    'Activer ou désactiver un compte utilisateur'),
    ('ADDRESS_LIST_VIEW_ALL', 'Lister toutes les adresses de la plateforme'),
    ('SUPPORT_TICKET_VIEW',   'Consulter les tickets support'),
    ('SUPPORT_TICKET_MANAGE', 'Changer le statut des tickets support');

-- ADMIN : toutes les 6 permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- SUPPORT_AGENT : uniquement SUPPORT_TICKET_VIEW et SUPPORT_TICKET_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPPORT_AGENT'
  AND p.code IN ('SUPPORT_TICKET_VIEW', 'SUPPORT_TICKET_MANAGE');
