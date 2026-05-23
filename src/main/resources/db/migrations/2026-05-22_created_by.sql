-- =============================================================================
-- Migration manuelle : ajout de l'utilisateur créateur sur les bons de commande,
-- les livraisons (client + fournisseur) et les factures.
--
-- Hibernate (ddl-auto=update) ajoutera automatiquement la colonne created_by_id
-- nullable et la clé étrangère vers UTILISATEUR au prochain démarrage.
-- Ce script documente l'intention et permet d'exécuter la modification
-- manuellement (ou de la rejouer sur un environnement où ddl-auto est désactivé).
-- =============================================================================

-- 1) BONS_COMMANDE
ALTER TABLE BONS_COMMANDE
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT NULL,
    ADD CONSTRAINT fk_bon_commande_created_by
        FOREIGN KEY (created_by_id) REFERENCES UTILISATEUR(id);

-- 2) FACTURES
ALTER TABLE FACTURES
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT NULL,
    ADD CONSTRAINT fk_facture_created_by
        FOREIGN KEY (created_by_id) REFERENCES UTILISATEUR(id);

-- 3) LIVRAISONS_CLIENT
ALTER TABLE LIVRAISONS_CLIENT
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT NULL,
    ADD CONSTRAINT fk_livraison_client_created_by
        FOREIGN KEY (created_by_id) REFERENCES UTILISATEUR(id);

-- 4) LIVRAISONS_FOURNISSEUR
ALTER TABLE LIVRAISONS_FOURNISSEUR
    ADD COLUMN IF NOT EXISTS created_by_id BIGINT NULL,
    ADD CONSTRAINT fk_livraison_fournisseur_created_by
        FOREIGN KEY (created_by_id) REFERENCES UTILISATEUR(id);
