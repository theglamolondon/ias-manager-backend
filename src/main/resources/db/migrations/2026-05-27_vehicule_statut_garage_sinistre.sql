-- =============================================================================
-- Migration : Refonte des statuts véhicule
--
-- Changements :
--   • PANNE     → GARAGE   (véhicule en maintenance/réparation, retour prévu)
--   • SINISTRE  (nouveau)  (dommages irréparables, perte définitive)
--   • INDISPONIBLE conservé mais redéfini : défaut de pièces administratives
--   • REFORME / DISPONIBLE / MISSION : inchangés
--
-- La colonne `statut` est en VARCHAR (EnumType.STRING via Hibernate).
-- Hibernate ajoutera GARAGE et SINISTRE comme valeurs acceptées au redémarrage
-- (ddl-auto=update ne modifie pas les contraintes CHECK sur les enums Java).
-- Ce script doit être exécuté AVANT le démarrage du backend pour éviter
-- toute erreur de désérialisation des lignes PANNE existantes.
-- =============================================================================

-- 1. Renommer les véhicules en PANNE → GARAGE
UPDATE vehicules
SET statut = 'GARAGE'
WHERE statut = 'PANNE';

-- 2. Vérification (résultat attendu : 0 lignes)
-- SELECT COUNT(*) FROM vehicules WHERE statut = 'PANNE';
