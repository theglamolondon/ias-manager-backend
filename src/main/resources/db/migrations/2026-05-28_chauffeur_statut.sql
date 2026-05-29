-- =============================================================================
-- Migration : Ajout du statut de disponibilité sur les chauffeurs
--
-- Nouveau champ :
--   • CHAUFFEURS.statut  VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
--     Valeurs : DISPONIBLE | MISSION | INDISPONIBLE
--
-- Comportement :
--   - Tous les chauffeurs existants sont initialisés à DISPONIBLE.
--   - Le statut passe automatiquement à MISSION au démarrage d'une mission
--     et revient à DISPONIBLE à la fin de la mission (géré par le backend).
--   - Seuls les chauffeurs DISPONIBLE peuvent être affectés à une mission.
-- =============================================================================

ALTER TABLE CHAUFFEURS
    ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE';

-- Backfill : s'assurer que toutes les lignes ont la valeur correcte.
UPDATE CHAUFFEURS SET statut = 'DISPONIBLE' WHERE statut IS NULL OR statut = '';
