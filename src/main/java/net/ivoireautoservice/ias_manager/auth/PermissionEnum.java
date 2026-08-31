package net.ivoireautoservice.ias_manager.auth;

/**
 * Catalogue des permissions atomiques du système (vocabulaire fixe, défini en code).
 *
 * <p>Le catalogue est <b>aligné sur le menu du frontend</b> : chaque entrée de menu
 * correspond à une ressource, et chaque ressource expose les actions pertinentes
 * (VOIR / CRÉER / MODIFIER / SUPPRIMER, plus quelques verbes métier comme VALIDER,
 * ANNULER, APPROUVER, SOLDER). La permission {@code *_READ} d'une ressource gouverne
 * la <b>visibilité du menu</b> ; les autres actions gouvernent les boutons à l'intérieur.</p>
 *
 * <p>Le vocabulaire vit en code (et non en base) pour qu'aucune permission « orpheline »
 * — qu'aucun {@code @PreAuthorize} n'applique — ne puisse exister. Les <b>rôles</b>
 * ({@code RoleEntity}) regroupent ces permissions et sont gérés dynamiquement par un admin.</p>
 *
 * <p>{@link #module} = groupe de menu (sert à construire l'arbre de cases à cocher de l'UI).</p>
 */
public enum PermissionEnum {

    // ===================== GÉNÉRAL =====================
    DASHBOARD_READ("GENERAL", "Tableau de bord", "Voir le tableau de bord"),

    // ===================== VÉHICULES =====================
    VEHICULE_READ("VEHICULES", "Véhicules", "Voir les véhicules"),
    VEHICULE_CREATE("VEHICULES", "Véhicules", "Créer un véhicule"),
    VEHICULE_UPDATE("VEHICULES", "Véhicules", "Modifier un véhicule"),
    VEHICULE_DELETE("VEHICULES", "Véhicules", "Supprimer un véhicule"),

    MISSION_READ("VEHICULES", "Missions", "Voir les missions"),
    MISSION_CREATE("VEHICULES", "Missions", "Créer une mission"),
    MISSION_UPDATE("VEHICULES", "Missions", "Modifier une mission"),
    MISSION_ANNULER("VEHICULES", "Missions", "Annuler une mission"),

    INTERVENTION_READ("VEHICULES", "Interventions", "Voir les interventions"),
    INTERVENTION_CREATE("VEHICULES", "Interventions", "Créer une intervention"),
    INTERVENTION_UPDATE("VEHICULES", "Interventions", "Modifier une intervention"),
    INTERVENTION_DELETE("VEHICULES", "Interventions", "Supprimer une intervention"),

    // ===================== RESSOURCES HUMAINES =====================
    EMPLOYE_READ("RH", "Employés", "Voir les employés"),
    EMPLOYE_CREATE("RH", "Employés", "Créer un employé"),
    EMPLOYE_UPDATE("RH", "Employés", "Modifier un employé"),
    EMPLOYE_DELETE("RH", "Employés", "Supprimer un employé"),

    CHAUFFEUR_READ("RH", "Chauffeurs", "Voir les chauffeurs"),
    CHAUFFEUR_CREATE("RH", "Chauffeurs", "Créer un chauffeur"),
    CHAUFFEUR_UPDATE("RH", "Chauffeurs", "Modifier un chauffeur"),
    CHAUFFEUR_DELETE("RH", "Chauffeurs", "Supprimer un chauffeur"),

    // ===================== STOCK =====================
    PRODUIT_READ("STOCK", "Produits", "Voir les produits"),
    PRODUIT_CREATE("STOCK", "Produits", "Créer un produit"),
    PRODUIT_UPDATE("STOCK", "Produits", "Modifier un produit"),
    PRODUIT_DELETE("STOCK", "Produits", "Supprimer un produit"),

    BON_COMMANDE_READ("STOCK", "Bons de commande", "Voir les bons de commande"),
    BON_COMMANDE_CREATE("STOCK", "Bons de commande", "Créer un bon de commande"),
    BON_COMMANDE_UPDATE("STOCK", "Bons de commande", "Modifier un bon de commande"),
    BON_COMMANDE_DELETE("STOCK", "Bons de commande", "Supprimer un bon de commande"),

    APPRO_READ("STOCK", "Approvisionnements", "Voir les approvisionnements"),
    APPRO_CREATE("STOCK", "Approvisionnements", "Créer un approvisionnement"),
    APPRO_UPDATE("STOCK", "Approvisionnements", "Modifier un approvisionnement"),
    APPRO_DELETE("STOCK", "Approvisionnements", "Supprimer un approvisionnement"),

    LIVRAISON_CLIENT_READ("STOCK", "Livraisons client", "Voir les livraisons client"),
    LIVRAISON_CLIENT_CREATE("STOCK", "Livraisons client", "Créer une livraison client"),
    LIVRAISON_CLIENT_UPDATE("STOCK", "Livraisons client", "Modifier une livraison client"),
    LIVRAISON_CLIENT_DELETE("STOCK", "Livraisons client", "Supprimer une livraison client"),

    // ===================== FINANCES =====================
    TRESORERIE_READ("FINANCES", "Trésorerie", "Voir la trésorerie"),
    TRESORERIE_CREATE("FINANCES", "Trésorerie", "Créer une opération de trésorerie"),
    TRESORERIE_UPDATE("FINANCES", "Trésorerie", "Modifier une opération de trésorerie"),
    TRESORERIE_APPROUVER("FINANCES", "Trésorerie", "Approuver une opération de trésorerie"),
    TRESORERIE_SOLDER("FINANCES", "Trésorerie", "Solder une opération de trésorerie"),
    TRESORERIE_ADMIN("FINANCES", "Trésorerie", "Administrer la trésorerie (tous les comptes)"),

    FACTURE_FOURNISSEUR_READ("FINANCES", "Factures fournisseur", "Voir les factures fournisseur"),
    FACTURE_FOURNISSEUR_CREATE("FINANCES", "Factures fournisseur", "Créer une facture fournisseur"),
    FACTURE_FOURNISSEUR_VALIDER("FINANCES", "Factures fournisseur", "Valider une facture fournisseur"),
    FACTURE_FOURNISSEUR_DELETE("FINANCES", "Factures fournisseur", "Supprimer une facture fournisseur"),

    FACTURE_CLIENT_READ("FINANCES", "Factures client", "Voir les factures client"),
    FACTURE_CLIENT_CREATE("FINANCES", "Factures client", "Créer une facture client"),
    FACTURE_CLIENT_VALIDER("FINANCES", "Factures client", "Valider une facture client"),
    FACTURE_CLIENT_DELETE("FINANCES", "Factures client", "Supprimer une facture client"),

    RAPPORT_READ("FINANCES", "Rapport financier", "Voir le rapport financier"),

    // ===================== PARTENAIRES =====================
    PARTENAIRE_READ("PARTENAIRES", "Partenaires", "Voir les partenaires"),
    PARTENAIRE_CREATE("PARTENAIRES", "Partenaires", "Créer un partenaire"),
    PARTENAIRE_UPDATE("PARTENAIRES", "Partenaires", "Modifier un partenaire"),
    PARTENAIRE_DELETE("PARTENAIRES", "Partenaires", "Supprimer un partenaire"),

    // ===================== CONFIGURATIONS (Paramètres) =====================
    PARAMETRE_READ("CONFIGURATION", "Paramètres", "Voir les paramètres et référentiels"),
    PARAMETRE_MANAGE("CONFIGURATION", "Paramètres", "Gérer les paramètres et référentiels"),

    UTILISATEUR_READ("CONFIGURATION", "Utilisateurs", "Voir les utilisateurs"),
    UTILISATEUR_MANAGE("CONFIGURATION", "Utilisateurs", "Gérer les utilisateurs et leurs accès"),

    ROLE_MANAGE("CONFIGURATION", "Rôles", "Gérer les rôles"),
    GROUPE_MANAGE("CONFIGURATION", "Groupes", "Gérer les groupes");

    private final String module;
    private final String ressource;
    private final String libelle;

    PermissionEnum(String module, String ressource, String libelle) {
        this.module = module;
        this.ressource = ressource;
        this.libelle = libelle;
    }

    /** Groupe de menu (GENERAL, VEHICULES, RH, STOCK, FINANCES, PARTENAIRES, CONFIGURATION). */
    public String getModule() {
        return module;
    }

    /** Entrée de menu / ressource concernée (Véhicules, Missions, ...). */
    public String getRessource() {
        return ressource;
    }

    /** Libellé lisible de l'action. */
    public String getLibelle() {
        return libelle;
    }
}
