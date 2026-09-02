-- Suppression des colonnes perdiem de la table missions.
-- Les perdiem chauffeur sont désormais des dépenses de mission (table depenses_mission).
-- À exécuter manuellement contre MySQL après déploiement du backend.

ALTER TABLE missions
    DROP COLUMN IF EXISTS perdiem,
    DROP COLUMN IF EXISTS total_perdiem;
