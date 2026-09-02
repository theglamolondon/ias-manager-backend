-- =============================================================================
-- Migration manuelle : statut sur livraisons_fournisseur + relation 1:N facture
--
-- À exécuter UNE SEULE FOIS sur la base de production (et toute base existante),
-- AVANT le redémarrage de l'application avec la nouvelle version.
--
-- Hibernate (ddl-auto=update) :
--   - ajoutera automatiquement les colonnes statut, date_validation, date_annulation
--     si elles n'existent pas — mais sans backfill (statut sera NULL pour les BL existants).
--   - NE supprimera PAS l'index unique pré-existant sur facture_id.
--
-- Ce script :
--   1. Backfill du statut pour les BL historiques (passent à VALIDE rétroactivement,
--      car ils ont déjà déclenché stock + facture dans l'ancien comportement).
--   2. Suppression de l'index unique sur facture_id (modèle 1:N facture↔BL).
-- =============================================================================

-- 1) Backfill statut des BL existants
--    Si la colonne n'existe pas encore (Hibernate ne l'a pas créée), commenter
--    cette section, démarrer une fois l'app pour qu'elle l'ajoute, puis la rejouer.
UPDATE livraisons_fournisseur
SET statut = 'VALIDE',
    date_validation = created_at
WHERE statut IS NULL OR statut = '';

-- 2) Suppression de l'index unique sur facture_id
--    Le nom de l'index dépend de la version d'Hibernate. Récupérer le bon nom avec :
--      SHOW INDEX FROM livraisons_fournisseur WHERE Non_unique = 0 AND Column_name = 'facture_id';
--    Puis remplacer 'UK_xxx' ci-dessous.
-- Exemple type Hibernate 6 :
--   ALTER TABLE livraisons_fournisseur DROP INDEX UKxxxxxxxxxxxxxxxxxxx;
--
-- Variante MySQL "à la demande" (si vous ne connaissez pas le nom) :
SET @idx_name := (
    SELECT INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'livraisons_fournisseur'
      AND COLUMN_NAME = 'facture_id'
      AND NON_UNIQUE = 0
    LIMIT 1
);
SET @sql := IF(@idx_name IS NOT NULL,
               CONCAT('ALTER TABLE livraisons_fournisseur DROP INDEX ', @idx_name),
               'SELECT "Aucun index unique sur facture_id à supprimer" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
