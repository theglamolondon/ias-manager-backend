-- =============================================================================
-- Migration manuelle : ajout de l'utilisateur créateur sur les bons de commande,
-- les livraisons (client + fournisseur) et les factures.
--
-- Hibernate (ddl-auto=update) ajoutera automatiquement la colonne created_by_id
-- nullable et la clé étrangère vers utilisateur au prochain démarrage.
-- Ce script documente l'intention et permet d'exécuter la modification
-- manuellement (ou de la rejouer sur un environnement où ddl-auto est désactivé).
-- =============================================================================

-- 1) bons_commande
ALTER TABLE bons_commande
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT NULL,
    ADD CONSTRAINT fk_bon_commande_created_by
        FOREIGN KEY (created_by_id) REFERENCES utilisateur(id);

-- 2) factures
ALTER TABLE factures
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT NULL,
    ADD CONSTRAINT fk_facture_created_by
        FOREIGN KEY (created_by_id) REFERENCES utilisateur(id);

-- 3) livraisons_client
ALTER TABLE livraisons_client
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT NULL,
    ADD CONSTRAINT fk_livraison_client_created_by
        FOREIGN KEY (created_by_id) REFERENCES utilisateur(id);

-- 4) livraisons_fournisseur
ALTER TABLE livraisons_fournisseur
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT NULL,
    ADD CONSTRAINT fk_livraison_fournisseur_created_by
        FOREIGN KEY (created_by_id) REFERENCES utilisateur(id);
