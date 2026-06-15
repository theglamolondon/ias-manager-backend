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

/**
 * Amorce le modèle RBAC au démarrage (idempotent).
 *
 * <ol>
 *   <li>Crée/maintient les <b>rôles système</b> issus de {@link RoleEnum}. Le rôle
 *       {@code ADMIN} reçoit systématiquement <b>toutes</b> les permissions (pour qu'une
 *       nouvelle {@link PermissionEnum} ajoutée en code soit immédiatement accordée à
 *       l'admin). Les autres rôles système ne sont créés que s'ils n'existent pas
 *       encore — afin de respecter les ajustements faits ensuite par un administrateur.</li>
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
        RoleEntity admin = seedRolesSysteme();
        backfillUtilisateursSansDroits(admin);
    }

    private RoleEntity seedRolesSysteme() {
        // ADMIN : toujours synchronisé sur l'ensemble complet des permissions.
        RoleEntity admin = roleRepository.findByNom(RoleEnum.ADMIN.name())
                .orElseGet(() -> nouveauRoleSysteme(RoleEnum.ADMIN.name(), "Administrateur"));
        admin.setSystemRole(true);
        admin.setLibelle(admin.getLibelle() == null ? "Administrateur" : admin.getLibelle());
        admin.setPermissions(EnumSet.allOf(PermissionEnum.class));
        admin = roleRepository.save(admin);

        // Autres rôles système : créés uniquement s'ils n'existent pas encore.
        for (RoleEnum role : RoleEnum.values()) {
            if (role == RoleEnum.ADMIN) {
                continue;
            }
            if (roleRepository.findByNom(role.name()).isEmpty()) {
                RoleEntity entity = nouveauRoleSysteme(role.name(), libelleParDefaut(role));
                entity.setPermissions(permissionsParDefaut(role));
                roleRepository.save(entity);
                log.info("RBAC: rôle système '{}' créé", role.name());
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
                    PermissionEnum.INTERVENTION_UPDATE, PermissionEnum.INTERVENTION_DELETE,
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
                    PermissionEnum.LIVRAISON_CLIENT_READ, PermissionEnum.LIVRAISON_CLIENT_CREATE,
                    PermissionEnum.RAPPORT_READ);
            case RECOUVREMENT -> EnumSet.of(
                    PermissionEnum.DASHBOARD_READ,
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
