-- =============================================================================
-- Migration manuelle : normalisation de la casse des noms de tables
--
-- CONTEXTE
--   Sous Linux, MySQL est sensible à la casse sur les noms de bases et de tables
--   (lower_case_table_names = 0 par défaut), alors que sous macOS/MAMP il ne l'est
--   pas (lower_case_table_names = 2). Le schéma historique mélangeait deux
--   conventions : tables métier en MAJUSCULES (VEHICULES, MISSIONS, FACTURES...)
--   et tables RBAC en minuscules (roles, groupes, utilisateur, utilisateur_roles...).
--   Tout le code (annotations @Table, @Formula, requêtes natives, scripts SQL) a été
--   normalisé en minuscules ; ce script aligne les bases EXISTANTES.
--
-- QUAND L'EXÉCUTER
--   UNE SEULE FOIS par base existante (prod + dev), pendant une fenêtre de
--   maintenance, application ARRÊTÉE, AVANT de redémarrer la nouvelle version.
--   Sur une base vierge : inutile, Hibernate créera directement les bons noms.
--
-- SÉCURITÉ
--   - Faire un mysqldump complet AVANT (voir « Sauvegarde » ci-dessous).
--   - Idempotent : relancé, il ne renomme rien s'il n'y a plus rien à renommer.
--   - foreign_key_checks est laissé à 1 VOLONTAIREMENT : c'est ce qui permet à
--     InnoDB de réécrire les définitions de clés étrangères vers les nouveaux
--     noms. Le désactiver laisserait des FK pointant vers des tables inexistantes.
--
-- Sauvegarde préalable :
--   mysqldump --single-transaction --routines --triggers ias_manager > ias_manager_avant_casse.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0) CONTRÔLE PRÉALABLE (facultatif) : lister ce qui va être renommé.
--    Ne modifie rien. Exécuter d'abord cette requête pour vérifier la liste.
-- -----------------------------------------------------------------------------
SELECT TABLE_NAME AS table_actuelle, LOWER(TABLE_NAME) AS table_cible
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND CAST(TABLE_NAME AS BINARY) <> CAST(LOWER(TABLE_NAME) AS BINARY)
ORDER BY TABLE_NAME;

-- -----------------------------------------------------------------------------
-- 1) RENOMMAGE
--    Construit un unique RENAME TABLE (atomique) pour toutes les tables dont le
--    nom stocké diffère de sa version minuscule. Le passage par un nom temporaire
--    (`xxx__ias_tmp`) est nécessaire pour que l'opération fonctionne aussi sur un
--    serveur insensible à la casse (macOS, lower_case_table_names = 2), où
--    « VEHICULES -> vehicules » serait sinon refusé (table déjà existante).
-- -----------------------------------------------------------------------------
SET SESSION group_concat_max_len = 1048576;

SET @renommages := (
    SELECT GROUP_CONCAT(
               CONCAT('`', TABLE_NAME, '` TO `', LOWER(TABLE_NAME), '__ias_tmp`, ',
                      '`', LOWER(TABLE_NAME), '__ias_tmp` TO `', LOWER(TABLE_NAME), '`')
               SEPARATOR ', ')
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_TYPE = 'BASE TABLE'
      AND CAST(TABLE_NAME AS BINARY) <> CAST(LOWER(TABLE_NAME) AS BINARY)
);

SET @sql := IF(@renommages IS NULL,
               'SELECT ''Aucune table à renommer : la base est déjà en minuscules'' AS info',
               CONCAT('RENAME TABLE ', @renommages));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2) VÉRIFICATION APRÈS COUP
--    a) Plus aucune table avec une majuscule (doit renvoyer 0 ligne).
--    b) Aucun reliquat de nom temporaire (doit renvoyer 0 ligne) : s'il en reste,
--       le RENAME a été interrompu — restaurer le dump et recommencer.
-- -----------------------------------------------------------------------------
SELECT TABLE_NAME AS reste_en_majuscules
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_TYPE = 'BASE TABLE'
  AND CAST(TABLE_NAME AS BINARY) <> CAST(LOWER(TABLE_NAME) AS BINARY);

SELECT TABLE_NAME AS reliquat_temporaire
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME LIKE '%\_\_ias\_tmp';

-- -----------------------------------------------------------------------------
-- 3) CONTRÔLE DES CLÉS ÉTRANGÈRES
--    Vérifie qu'aucune FK ne pointe encore vers un nom en majuscules.
--    Doit renvoyer 0 ligne.
-- -----------------------------------------------------------------------------
SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME IS NOT NULL
  AND CAST(REFERENCED_TABLE_NAME AS BINARY) <> CAST(LOWER(REFERENCED_TABLE_NAME) AS BINARY);
