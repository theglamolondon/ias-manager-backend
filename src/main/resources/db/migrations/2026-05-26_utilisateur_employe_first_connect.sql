-- =============================================================================
-- Migration manuelle : ajout du lien optionnel Utilisateur -> Employé
-- et du flag hasChangePassword (changement de mot de passe à la 1ère connexion).
--
-- Hibernate (ddl-auto=update) ajoutera automatiquement :
--   * la colonne has_change_password (BOOLEAN, nullable par défaut)
--   * la colonne employe_id (BIGINT, nullable) et la FK vers EMPLOYES
-- au prochain démarrage.
--
-- Ce script documente l'intention et permet d'exécuter la modification
-- manuellement (ou de la rejouer sur un environnement où ddl-auto est désactivé).
-- Il contient aussi le backfill nécessaire pour ne pas forcer le changement
-- de mot de passe sur les utilisateurs déjà existants.
-- =============================================================================

-- 1) Lien optionnel vers l'employé
ALTER TABLE UTILISATEUR
    ADD COLUMN IF NOT EXISTS employe_id BIGINT NULL,
    ADD CONSTRAINT fk_utilisateur_employe
        FOREIGN KEY (employe_id) REFERENCES EMPLOYES(id);

-- 2) Flag « a déjà changé son mot de passe »
ALTER TABLE UTILISATEUR
    ADD COLUMN IF NOT EXISTS has_change_password BIT(1) NULL;

-- 3) Backfill : les comptes existants ont déjà un mot de passe choisi,
--    on les considère comme ayant déjà changé leur mot de passe initial.
UPDATE UTILISATEUR SET has_change_password = 1 WHERE has_change_password IS NULL;
