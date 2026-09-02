package net.ivoireautoservice.ias_manager.config;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.auth.RoleEnum;
import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.repository.RoleRepository;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Amorce le modèle RBAC au démarrage (idempotent).
 *
 * <ol>
 *   <li>Crée/maintient les <b>rôles système</b> issus de {@link RoleEnum}. Le rôle
 *       {@code ADMIN} reçoit systématiquement <b>toutes</b> les permissions (pour qu'une
 *       nouvelle {@link PermissionEnum} ajoutée en code soit immédiatement accordée à
 *       l'admin). Les autres rôles système sont créés s'ils manquent, puis
 *       <b>resynchronisés sur leur préréglage</b> à chaque démarrage : leurs permissions
 *       étant figées côté API ({@code RoleService#updateRole}), le code reste la seule
 *       source de vérité et une permission ajoutée à un préréglage arrive en base sans
 *       intervention manuelle. Les rôles <i>non</i> système (créés par un admin) ne sont
 *       jamais touchés.</li>
 *   <li>Les valeurs de {@code role_permissions} qui n'existent plus dans
 *       {@link PermissionEnum} (permission renommée/supprimée) sont purgées avant tout
 *       chargement de rôle.</li>
 *   <li><b>Bascule de sécurité (one-time)</b> : si <i>aucun</i> utilisateur ne possède
 *       de rôle ni de groupe (cas du tout premier démarrage après l'introduction du
 *       RBAC), on attribue le rôle {@code ADMIN} à tous les utilisateurs existants pour
 *       éviter un verrouillage total. Dès qu'au moins un droit est attribué quelque part,
 *       cette bascule ne se redéclenche plus.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class RbacSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RbacSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        purgerPermissionsObsoletes();
        RoleEntity admin = seedRolesSysteme();
        backfillUtilisateursSansDroits(admin);
    }

    /**
     * Nettoie les permissions présentes en base mais disparues du code (renommage ou
     * suppression d'une valeur de {@link PermissionEnum}). Sans ce nettoyage, le
     * chargement du rôle concerné échouerait sur une valeur d'enum inconnue.
     */
    private void purgerPermissionsObsoletes() {
        Set<String> connues = EnumSet.allOf(PermissionEnum.class).stream()
                .map(PermissionEnum::name)
                .collect(Collectors.toSet());
        // Les valeurs à supprimer sont calculées ici puis énumérées explicitement dans
        // le DELETE. Surtout pas un « DELETE ... WHERE permission NOT IN (catalogue) » :
        // au moindre problème de liaison du paramètre, cette forme viderait toute la
        // table et effacerait silencieusement les droits des rôles non système.
        List<String> obsoletes = roleRepository.findPermissionsDistinctes().stream()
                .filter(nom -> !connues.contains(nom))
                .toList();
        if (obsoletes.isEmpty()) {
            return;
        }
        int supprimees = roleRepository.supprimerPermissions(obsoletes);
        log.warn("RBAC: {} attribution(s) supprimée(s) pour {} permission(s) disparue(s) du code : {}",
                supprimees, obsoletes.size(), obsoletes);
    }

    private RoleEntity seedRolesSysteme() {
        // ADMIN : toujours synchronisé sur l'ensemble complet des permissions.
        RoleEntity admin = roleRepository.findByNom(RoleEnum.ADMIN.name())
                .orElseGet(() -> nouveauRoleSysteme(RoleEnum.ADMIN.name(), "Administrateur"));
        admin.setSystemRole(true);
        admin.setLibelle(admin.getLibelle() == null ? "Administrateur" : admin.getLibelle());
        admin.setPermissions(EnumSet.allOf(PermissionEnum.class));
        admin = roleRepository.save(admin);

        // Autres rôles système : créés s'ils n'existent pas, puis resynchronisés sur
        // leur préréglage. Les permissions d'un rôle système sont figées côté API
        // (cf. RoleService#updateRole) : le code est donc la seule source de vérité,
        // et une permission ajoutée à un préréglage doit atteindre la base au démarrage.
        for (RoleEnum role : RoleEnum.values()) {
            if (role == RoleEnum.ADMIN) {
                continue;
            }
            RoleEntity entity = roleRepository.findByNom(role.name()).orElse(null);
            boolean creation = entity == null;
            if (creation) {
                entity = nouveauRoleSysteme(role.name(), libelleParDefaut(role));
            }
            Set<PermissionEnum> attendues = permissionsParDefaut(role);
            boolean desynchronise = !attendues.equals(entity.getPermissions());
            if (creation || desynchronise) {
                entity.setSystemRole(true);
                entity.setPermissions(attendues);
                roleRepository.save(entity);
                log.info("RBAC: rôle système '{}' {}", role.name(), creation ? "créé" : "resynchronisé");
            }
        }
        return admin;
    }

    private void backfillUtilisateursSansDroits(RoleEntity admin) {
        List<Utilisateur> users = userRepository.findAll();
        if (users.isEmpty()) {
            return;
        }
        boolean aucunDroitAttribue = users.stream()
                .allMatch(u -> u.getRoles().isEmpty() && u.getGroupes().isEmpty());
        if (!aucunDroitAttribue) {
            return;
        }
        for (Utilisateur user : users) {
            Set<RoleEntity> roles = new HashSet<>(user.getRoles());
            roles.add(admin);
            user.setRoles(roles);
        }
        userRepository.saveAll(users);
        log.warn("RBAC: bascule initiale — rôle ADMIN attribué à {} utilisateur(s) sans droits", users.size());
    }

    private RoleEntity nouveauRoleSysteme(String nom, String libelle) {
        RoleEntity entity = new RoleEntity();
        entity.setNom(nom);
        entity.setLibelle(libelle);
        entity.setSystemRole(true);
        entity.setPermissions(new HashSet<>());
        return entity;
    }

    private String libelleParDefaut(RoleEnum role) {
        return switch (role) {
            case ADMIN -> "Administrateur";
            case LOGISTIQUE -> "Logistique";
            case COMMERCIAL -> "Commercial";
            case RECOUVREMENT -> "Recouvrement";
            case RH -> "Ressources humaines";
            case ACHAT -> "Achats";
        };
    }

    private Set<PermissionEnum> permissionsParDefaut(RoleEnum role) {
        return switch (role) {
            case ADMIN -> EnumSet.allOf(PermissionEnum.class);
            case LOGISTIQUE -> EnumSet.of(
                    PermissionEnum.DASHBOARD_READ,
                    PermissionEnum.VEHICULE_READ, PermissionEnum.VEHICULE_CREATE,
                    PermissionEnum.VEHICULE_UPDATE, PermissionEnum.VEHICULE_DELETE,
                    PermissionEnum.MISSION_READ, PermissionEnum.MISSION_CREATE,
                    PermissionEnum.MISSION_UPDATE, PermissionEnum.MISSION_ANNULER,
                    PermissionEnum.INTERVENTION_READ, PermissionEnum.INTERVENTION_CREATE,
                    PermissionEnum.INTERVENTION_UPDATE, PermissionEnum.INTERVENTION_PAYER,
                    PermissionEnum.INTERVENTION_DELETE,
                    PermissionEnum.CHAUFFEUR_READ,
                    PermissionEnum.LIVRAISON_CLIENT_READ, PermissionEnum.LIVRAISON_CLIENT_CREATE,
                    PermissionEnum.LIVRAISON_CLIENT_UPDATE, PermissionEnum.LIVRAISON_CLIENT_DELETE);
            case COMMERCIAL -> EnumSet.of(
                    PermissionEnum.DASHBOARD_READ,
                    PermissionEnum.FACTURE_CLIENT_READ, PermissionEnum.FACTURE_CLIENT_CREATE,
                    PermissionEnum.FACTURE_CLIENT_VALIDER,
                    PermissionEnum.PARTENAIRE_READ, PermissionEnum.PARTENAIRE_CREATE,
                    PermissionEnum.PARTENAIRE_UPDATE,
                    PermissionEnum.MISSION_READ,
                    PermissionEnum.LIVRAISON_CLIENT_READ, PermissionEnum.LIVRAISON_CLIENT_CREATE);
            case RECOUVREMENT -> EnumSet.of(
                    PermissionEnum.DASHBOARD_READ,
                    PermissionEnum.INTERVENTION_READ, PermissionEnum.INTERVENTION_PAYER,
                    PermissionEnum.FACTURE_CLIENT_READ, PermissionEnum.FACTURE_CLIENT_VALIDER,
                    PermissionEnum.FACTURE_FOURNISSEUR_READ, PermissionEnum.FACTURE_FOURNISSEUR_VALIDER,
                    PermissionEnum.TRESORERIE_READ, PermissionEnum.TRESORERIE_CREATE,
                    PermissionEnum.TRESORERIE_UPDATE, PermissionEnum.TRESORERIE_APPROUVER,
                    PermissionEnum.TRESORERIE_SOLDER,
                    PermissionEnum.RAPPORT_READ);
            case RH -> EnumSet.of(
                    PermissionEnum.DASHBOARD_READ,
                    PermissionEnum.EMPLOYE_READ, PermissionEnum.EMPLOYE_CREATE,
                    PermissionEnum.EMPLOYE_UPDATE, PermissionEnum.EMPLOYE_DELETE,
                    PermissionEnum.CHAUFFEUR_READ, PermissionEnum.CHAUFFEUR_CREATE,
                    PermissionEnum.CHAUFFEUR_UPDATE, PermissionEnum.CHAUFFEUR_DELETE);
            case ACHAT -> EnumSet.of(
                    PermissionEnum.DASHBOARD_READ,
                    PermissionEnum.BON_COMMANDE_READ, PermissionEnum.BON_COMMANDE_CREATE,
                    PermissionEnum.BON_COMMANDE_UPDATE, PermissionEnum.BON_COMMANDE_DELETE,
                    PermissionEnum.APPRO_READ, PermissionEnum.APPRO_CREATE,
                    PermissionEnum.APPRO_UPDATE, PermissionEnum.APPRO_DELETE,
                    PermissionEnum.PRODUIT_READ, PermissionEnum.PRODUIT_CREATE,
                    PermissionEnum.PRODUIT_UPDATE, PermissionEnum.PRODUIT_DELETE,
                    PermissionEnum.FACTURE_FOURNISSEUR_READ, PermissionEnum.FACTURE_FOURNISSEUR_CREATE,
                    PermissionEnum.PARTENAIRE_READ);
        };
    }
}
