-- =============================================================================
-- Migration : Ajout du statut de disponibilité sur les chauffeurs
--
-- Nouveau champ :
--   • chauffeurs.statut  VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
--     Valeurs : DISPONIBLE | MISSION | INDISPONIBLE
--
-- Comportement :
--   - Tous les chauffeurs existants sont initialisés à DISPONIBLE.
--   - Le statut passe automatiquement à MISSION au démarrage d'une mission
--     et revient à DISPONIBLE à la fin de la mission (géré par le backend).
--   - Seuls les chauffeurs DISPONIBLE peuvent être affectés à une mission.
-- =============================================================================

ALTER TABLE chauffeurs
    ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE';

-- Backfill : s'assurer que toutes les lignes ont la valeur correcte.
UPDATE chauffeurs SET statut = 'DISPONIBLE' WHERE statut IS NULL OR statut = '';
