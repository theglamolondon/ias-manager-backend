-- Périmètre de trésorerie par utilisateur.
--
-- AUCUNE MIGRATION DE SCHÉMA N'EST NÉCESSAIRE. Ce fichier documente le changement
-- et fournit les requêtes de vérification à passer contre MySQL.
--
-- 1) TRESORERIE_READ devient une permission DÉRIVÉE : elle est accordée
--    automatiquement à tout utilisateur rattaché à au moins un compte (affecté
--    dans compte_utilisateurs ou manager d'un compte via comptes.utilisateur_id),
--    et retirée dès que ce rattachement disparaît. Rien n'est stocké : le calcul
--    est fait par une formule Hibernate sur l'entité Utilisateur, et le filtre
--    JWT recharge l'utilisateur à chaque requête (effet immédiat, sans
--    reconnexion). Elle reste par ailleurs attribuable manuellement via un rôle.
--
-- 2) Nouvelle permission TRESORERIE_ADMIN (trésorier en chef) : accès à tous les
--    comptes en lecture + administration des comptes. Elle est ajoutée au rôle
--    ADMIN automatiquement au démarrage par RbacSeeder ; pour l'accorder à un
--    autre rôle, passer par l'UI d'administration des rôles.
--
--    ⚠ Les routes de création/modification d'un compte passent de
--    TRESORERIE_CREATE / TRESORERIE_UPDATE à TRESORERIE_ADMIN : les rôles non
--    ADMIN qui créaient des comptes (p. ex. RECOUVREMENT) doivent recevoir
--    TRESORERIE_ADMIN pour continuer à le faire.

-- Vérification : les sous-requêtes de périmètre s'appuient sur un index menant
-- par utilisateur_id. InnoDB en crée un automatiquement pour chaque clé
-- étrangère, ces deux requêtes doivent donc déjà retourner une ligne portant sur
-- utilisateur_id (Seq_in_index = 1). Si ce n'est pas le cas, créer les index.
--
--   SHOW INDEX FROM compte_utilisateurs;
--   SHOW INDEX FROM comptes;
--
--   CREATE INDEX idx_compte_utilisateurs_utilisateur ON compte_utilisateurs (utilisateur_id);
--   CREATE INDEX idx_comptes_manager ON comptes (utilisateur_id);

-- Contrôle du résultat de la permission dérivée (liste des utilisateurs qui
-- obtiennent TRESORERIE_READ du seul fait de leur rattachement à un compte) :
SELECT u.id, u.email
FROM utilisateur u
WHERE EXISTS (SELECT 1 FROM compte_utilisateurs cu WHERE cu.utilisateur_id = u.id)
   OR EXISTS (SELECT 1 FROM comptes c WHERE c.utilisateur_id = u.id);
