-- =============================================================================
-- Migration manuelle : imputation analytique des dépenses de trésorerie
--
-- CONTEXTE
--   Une dépense de caisse ne portait jusqu'ici ni nature ni rattachement : le coût
--   réel d'un véhicule (carburant, péage, assurance, gardiennage) n'apparaissait
--   nulle part. Les dépenses de mission, censées couvrir une partie du besoin,
--   n'ont jamais été branchées côté application — la table DEPENSES_MISSION est
--   restée vide et le modèle est remplacé par l'imputation portée directement par
--   la ligne de compte.
--
--   La colonne `origine` matérialise la règle qui évite le double comptage :
--   chaque dépense a UN porteur de sa valeur analytique. L'intervention porte son
--   coût (dès la clôture, réglée ou non), la facture porte le sien, la ligne
--   saisie à la main porte le sien. Les agrégats de coût véhicule ne somment que
--   les lignes MANUELLE ; les lignes INTERVENTION et FACTURE sont des mouvements
--   de caisse dont la valeur est déjà comptée ailleurs.
--
-- QUAND L'EXÉCUTER
--   UNE SEULE FOIS par base existante (prod + dev), pendant une fenêtre de
--   maintenance, application ARRÊTÉE, AVANT de démarrer la nouvelle version.
--   Hibernate (ddl-auto=update) créerait bien les colonnes, mais ni le
--   rétro-remplissage d'`origine`, ni la suppression de table, ni l'unicité.
--
--   Sur une base vierge : seules les sections 5 et 6 sont utiles.
--
-- Sauvegarde préalable :
--   mysqldump --single-transaction --routines --triggers ias_manager > ias_manager_avant_imputation.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) COLONNES D'IMPUTATION SUR LES MOUVEMENTS DE TRÉSORERIE
--    Toutes nullables : un approvisionnement n'a pas d'imputation, et les
--    mouvements historiques n'en ont jamais eu.
-- -----------------------------------------------------------------------------
ALTER TABLE lignes_compte
    ADD COLUMN type_depense_id BIGINT NULL AFTER type,
    ADD COLUMN vehicule_id     BIGINT NULL AFTER type_depense_id,
    ADD COLUMN mission_id      BIGINT NULL AFTER vehicule_id,
    ADD COLUMN origine         VARCHAR(20) NULL AFTER mission_id;

-- -----------------------------------------------------------------------------
-- 2) RÉTRO-REMPLISSAGE DE L'ORIGINE
--    Ordre important : le cas le plus spécifique d'abord.
--
--    Les lignes de facture sont reconnues de façon certaine par facture_id.
--    Les lignes d'intervention n'ont pas de lien vers leur source : elles sont
--    reconnues par l'objet, que le code génère sous la forme
--    « INTERVENTION <type> — <immatriculation> ». Heuristique, mais sans risque
--    sur les agrégats : ceux-ci ne comptent que les lignes portant un
--    vehicule_id, et aucune ligne existante n'en a. Seule la lisibilité de la
--    vue trésorerie est en jeu.
-- -----------------------------------------------------------------------------
UPDATE lignes_compte SET origine = 'FACTURE'     WHERE facture_id IS NOT NULL;
UPDATE lignes_compte SET origine = 'INTERVENTION' WHERE origine IS NULL AND objet LIKE 'INTERVENTION %';
UPDATE lignes_compte SET origine = 'MANUELLE'     WHERE origine IS NULL;

ALTER TABLE lignes_compte MODIFY COLUMN origine VARCHAR(20) NOT NULL;

-- -----------------------------------------------------------------------------
-- 3) CLÉS ÉTRANGÈRES ET INDEX
--    Les index servent les agrégats de la fiche véhicule et les filtres de la
--    vue trésorerie, tous interrogés par imputation.
-- -----------------------------------------------------------------------------
ALTER TABLE lignes_compte
    ADD CONSTRAINT fk_lignes_compte_type_depense FOREIGN KEY (type_depense_id) REFERENCES types_depense (id),
    ADD CONSTRAINT fk_lignes_compte_vehicule     FOREIGN KEY (vehicule_id)     REFERENCES vehicules (id),
    ADD CONSTRAINT fk_lignes_compte_mission      FOREIGN KEY (mission_id)      REFERENCES missions (id);

CREATE INDEX idx_lignes_compte_vehicule     ON lignes_compte (vehicule_id);
CREATE INDEX idx_lignes_compte_mission      ON lignes_compte (mission_id);
CREATE INDEX idx_lignes_compte_type_depense ON lignes_compte (type_depense_id);
CREATE INDEX idx_lignes_compte_origine      ON lignes_compte (origine, type);

-- -----------------------------------------------------------------------------
-- 4) COHÉRENCE DE L'IMPUTATION
--    Une mission implique toujours son véhicule : le véhicule est figé au moment
--    de la dépense, car celui d'une mission peut changer en cours de route
--    (action « Changer de véhicule ») et une résolution à la lecture déplacerait
--    rétroactivement des frais déjà engagés.
-- -----------------------------------------------------------------------------
ALTER TABLE lignes_compte
    ADD CONSTRAINT chk_lignes_compte_imputation
    CHECK (mission_id IS NULL OR vehicule_id IS NOT NULL);

-- -----------------------------------------------------------------------------
-- 5) SUPPRESSION DU MODÈLE REMPLACÉ
--    Contrôle préalable : la table doit être vide. Si ce SELECT renvoie autre
--    chose que 0, NE PAS poursuivre — des dépenses auraient été créées par appel
--    direct à l'API et devraient être reprises en lignes de compte au préalable.
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS depenses_mission_restantes FROM depenses_mission;

DROP TABLE IF EXISTS depenses_mission;

-- -----------------------------------------------------------------------------
-- 6) RÉFÉRENTIEL DES TYPES DE DÉPENSE
--    Le seeder applicatif contrôle l'existence par libellé exact : sans unicité
--    en base, deux libellés identiques rendraient la ventilation par nature
--    ininterprétable. Doublons à traiter avant de poser la contrainte.
-- -----------------------------------------------------------------------------
SELECT libelle, COUNT(*) AS occurrences
FROM types_depense
GROUP BY libelle
HAVING COUNT(*) > 1;

ALTER TABLE types_depense ADD CONSTRAINT uk_types_depense_libelle UNIQUE (libelle);

-- -----------------------------------------------------------------------------
-- 7) CONTRÔLE FINAL (facultatif) : répartition des mouvements par origine.
-- -----------------------------------------------------------------------------
SELECT origine, type, COUNT(*) AS nombre, COALESCE(SUM(montant), 0) AS total
FROM lignes_compte
GROUP BY origine, type
ORDER BY origine, type;
