-- =============================================================================
-- Migration : Remplacement de is_interieur par localisation sur les missions
--
-- Ancien champ :
--   • missions.is_interieur  BOOLEAN (true = intérieur du pays, false/null = extérieur)
--
-- Nouveau champ :
--   • missions.localisation  VARCHAR(20)
--     Valeurs : VILLE | INTERIEUR | EXTERIEUR
--
-- Sémantique :
--   - VILLE     : mission dans la ville (Abidjan) — pas de supplément
--   - INTERIEUR : mission à l'intérieur du pays, hors ville
--   - EXTERIEUR : mission hors du pays
--
-- Règle de backfill :
--   - is_interieur = true  → INTERIEUR
--   - is_interieur = false → EXTERIEUR
--   - is_interieur = null  → VILLE  (était traité comme extérieur à tort)
-- =============================================================================

ALTER TABLE missions
    ADD COLUMN localisation VARCHAR(20);

UPDATE missions
SET localisation = CASE
    WHEN is_interieur = true  THEN 'INTERIEUR'
    WHEN is_interieur = false THEN 'EXTERIEUR'
    ELSE 'VILLE'
END;

ALTER TABLE missions
    DROP COLUMN is_interieur;
