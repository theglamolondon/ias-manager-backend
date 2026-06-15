-- =============================================================================
-- Migration manuelle : modèle RBAC à 3 niveaux (Permission ⊂ Rôle ⊂ Groupe).
--
-- Hibernate (ddl-auto=update) crée automatiquement ces tables au prochain
-- démarrage à partir des entités RoleEntity / GroupeEntity / Utilisateur. Ce
-- script documente le schéma et permet de l'appliquer manuellement (ou de le
-- rejouer là où ddl-auto est désactivé).
--
-- L'amorçage des rôles système (ADMIN, LOGISTIQUE, ...) et la bascule de
-- sécurité (attribution d'ADMIN aux utilisateurs existants sans droits) sont
-- réalisés automatiquement et de façon idempotente par RbacSeeder au démarrage.
-- =============================================================================

-- 1) Rôles : paquet de permissions
CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nom         VARCHAR(255) NOT NULL,
    libelle     VARCHAR(255) NULL,
    description VARCHAR(500) NULL,
    system_role BIT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NULL,
    updated_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_nom UNIQUE (nom)
);

-- 2) Permissions d'un rôle (ElementCollection d'enum)
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id    BIGINT       NOT NULL,
    permission VARCHAR(255) NOT NULL,
    PRIMARY KEY (role_id, permission),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 3) Groupes : paquet de rôles
CREATE TABLE IF NOT EXISTS groupes (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nom         VARCHAR(255) NOT NULL,
    description VARCHAR(500) NULL,
    created_at  DATETIME(6)  NULL,
    updated_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_groupes_nom UNIQUE (nom)
);

-- 4) Association Groupe <-> Rôle
CREATE TABLE IF NOT EXISTS groupe_roles (
    groupe_id BIGINT NOT NULL,
    role_id   BIGINT NOT NULL,
    PRIMARY KEY (groupe_id, role_id),
    CONSTRAINT fk_groupe_roles_groupe
        FOREIGN KEY (groupe_id) REFERENCES groupes(id) ON DELETE CASCADE,
    CONSTRAINT fk_groupe_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 5) Association Utilisateur <-> Rôle (rôles directs)
CREATE TABLE IF NOT EXISTS utilisateur_roles (
    utilisateur_id BIGINT NOT NULL,
    role_id        BIGINT NOT NULL,
    PRIMARY KEY (utilisateur_id, role_id),
    CONSTRAINT fk_utilisateur_roles_user
        FOREIGN KEY (utilisateur_id) REFERENCES UTILISATEUR(id) ON DELETE CASCADE,
    CONSTRAINT fk_utilisateur_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 6) Association Utilisateur <-> Groupe
CREATE TABLE IF NOT EXISTS utilisateur_groupes (
    utilisateur_id BIGINT NOT NULL,
    groupe_id      BIGINT NOT NULL,
    PRIMARY KEY (utilisateur_id, groupe_id),
    CONSTRAINT fk_utilisateur_groupes_user
        FOREIGN KEY (utilisateur_id) REFERENCES UTILISATEUR(id) ON DELETE CASCADE,
    CONSTRAINT fk_utilisateur_groupes_groupe
        FOREIGN KEY (groupe_id) REFERENCES groupes(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- Filet manuel (équivalent du backfill RbacSeeder), à n'exécuter QUE si l'on
-- n'utilise pas le seeder applicatif. Attribue le rôle ADMIN à tout utilisateur
-- ne possédant encore ni rôle ni groupe.
-- -----------------------------------------------------------------------------
-- INSERT INTO utilisateur_roles (utilisateur_id, role_id)
-- SELECT u.id, (SELECT id FROM roles WHERE nom = 'ADMIN')
-- FROM UTILISATEUR u
-- WHERE NOT EXISTS (SELECT 1 FROM utilisateur_roles ur WHERE ur.utilisateur_id = u.id)
--   AND NOT EXISTS (SELECT 1 FROM utilisateur_groupes ug WHERE ug.utilisateur_id = u.id);
