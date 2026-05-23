-- =============================================================================
-- Migration manuelle : annulation de mission + facture avoir + remboursement
--
-- À exécuter UNE SEULE FOIS sur la base de production, AVANT le redémarrage de
-- l'application avec la nouvelle version.
--
-- Hibernate (ddl-auto=update) ajoutera automatiquement les colonnes nullable
-- ci-dessous, mais cette migration documente l'intention et permet d'exécuter
-- la modification manuellement si nécessaire.
-- =============================================================================

-- 1) Colonnes d'annulation sur MISSIONS
ALTER TABLE MISSIONS
    ADD COLUMN IF NOT EXISTS dhms_annulation TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS motif_annulation TEXT NULL;

-- 2) Lien facture sur LIGNES_COMPTE (traçabilité paiement/remboursement)
ALTER TABLE LIGNES_COMPTE
    ADD COLUMN IF NOT EXISTS facture_id BIGINT NULL,
    ADD CONSTRAINT fk_ligne_compte_facture
        FOREIGN KEY (facture_id) REFERENCES FACTURES(id);

-- 3) Lien facture d'origine sur FACTURES (un avoir pointe vers la facture annulée)
ALTER TABLE FACTURES
    ADD COLUMN IF NOT EXISTS facture_origine_id BIGINT NULL,
    ADD CONSTRAINT fk_facture_origine
        FOREIGN KEY (facture_origine_id) REFERENCES FACTURES(id);
