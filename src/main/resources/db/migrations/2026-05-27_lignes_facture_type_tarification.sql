-- =============================================================================
-- Migration manuelle : ajout de la colonne type_tarification sur LIGNES_FACTURE.
--
-- Permet de connaître l'unité de durée appliquée à la ligne (JOURNALIERE,
-- MENSUELLE, UNIQUE, INDEFINIE) afin d'afficher correctement la « Période »
-- dans le PDF de facture location (Jours, Mois, Forfait...) sans devoir
-- la déduire de la désignation textuelle.
--
-- Hibernate (ddl-auto=update) ajoutera automatiquement la colonne
-- type_tarification (VARCHAR, nullable) au prochain démarrage.
-- Ce script documente l'intention et permet de rejouer le DDL manuellement.
-- =============================================================================

ALTER TABLE LIGNES_FACTURE
    ADD COLUMN IF NOT EXISTS type_tarification VARCHAR(20) NULL;
