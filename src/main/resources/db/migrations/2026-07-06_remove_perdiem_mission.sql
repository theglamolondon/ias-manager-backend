-- Suppression des colonnes perdiem de la table MISSIONS.
-- Les perdiem chauffeur sont désormais des dépenses de mission (table DEPENSES_MISSION).
-- À exécuter manuellement contre MySQL après déploiement du backend.

ALTER TABLE MISSIONS
    DROP COLUMN IF EXISTS perdiem,
    DROP COLUMN IF EXISTS total_perdiem;
