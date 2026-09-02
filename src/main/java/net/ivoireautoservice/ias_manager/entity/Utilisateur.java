package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, exclude = {"employe", "roles", "groupes"})
@ToString(exclude = {"employe", "roles", "groupes"})
public class Utilisateur extends AuditableEntity implements UserDetails {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String adresse;
    private String telephone;
    private String password;

    /**
     * Indique si l'utilisateur a déjà changé son mot de passe initial.
     * Mis à false lors d'une création ou d'une réinitialisation par un admin,
     * et à true dès que l'utilisateur change son mot de passe lui-même.
     */
    private Boolean hasChangePassword;

    /**
     * Lien optionnel vers l'employé associé à ce compte utilisateur.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id")
    private EmployeEntity employe;

    /**
     * Rôles attribués directement à l'utilisateur (modèle RBAC : niveau 2).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "utilisateur_roles",
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @BatchSize(size = 30)
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    /**
     * Groupes auxquels l'utilisateur appartient (modèle RBAC : niveau 3).
     * L'utilisateur hérite de tous les rôles de ces groupes.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "utilisateur_groupes",
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "groupe_id")
    )
    @BatchSize(size = 30)
    @Builder.Default
    private Set<GroupeEntity> groupes = new HashSet<>();

    /**
     * Vrai si l'utilisateur est rattaché à au moins un compte de trésorerie —
     * soit comme utilisateur affecté (COMPTE_UTILISATEURS), soit comme manager
     * du compte (COMPTES.utilisateur_id).
     *
     * <p>Champ <b>dérivé</b> (lecture seule, aucune colonne en base) : il pilote
     * l'octroi automatique de {@link PermissionEnum#TRESORERIE_READ} (cf.
     * {@link #getPermissionNames()}), qui est donc accordée puis retirée sans
     * aucune synchronisation à maintenir. L'effet est immédiat : le filtre JWT
     * recharge l'utilisateur depuis la base à chaque requête.</p>
     */
    @Formula("(case when exists (select 1 from compte_utilisateurs cu where cu.utilisateur_id = id) "
            + "or exists (select 1 from comptes c where c.utilisateur_id = id) then true else false end)")
    private boolean rattacheACompte;

    // ------------------------------------------------------------------
    // RBAC : dérivation des rôles / permissions effectifs
    // ------------------------------------------------------------------

    /**
     * Rôles effectifs = rôles directs ∪ rôles hérités des groupes.
     * La déduplication se fait par id (cf. {@code RoleEntity#equals}).
     *
     * <p>Tolérant aux collections nulles : un {@code Utilisateur} fraîchement
     * construit (no-arg + setters, via MapStruct/Jackson) peut avoir des
     * collections nulles à cause de l'interaction Lombok {@code @Builder.Default}.</p>
     */
    @Transient
    public Set<RoleEntity> getEffectiveRoles() {
        Set<RoleEntity> effective = new HashSet<>();
        if (roles != null) {
            effective.addAll(roles);
        }
        if (groupes != null) {
            for (GroupeEntity groupe : groupes) {
                if (groupe.getRoles() != null) {
                    effective.addAll(groupe.getRoles());
                }
            }
        }
        return effective;
    }

    /** Noms des rôles effectifs (triés, pour un affichage déterministe). */
    @Transient
    public Set<String> getRoleNames() {
        return getEffectiveRoles().stream()
                .map(RoleEntity::getNom)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Noms des groupes de l'utilisateur. */
    @Transient
    public Set<String> getGroupeNames() {
        if (groupes == null) {
            return new TreeSet<>();
        }
        return groupes.stream()
                .map(GroupeEntity::getNom)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Identifiants des rôles attribués <b>directement</b> à l'utilisateur
     * (hors rôles hérités des groupes). Permet à l'UI de gérer l'attribution
     * sans avoir à reconstruire la distinction direct / hérité côté frontend.
     */
    @Transient
    public Set<Long> getRoleIds() {
        if (roles == null) {
            return new HashSet<>();
        }
        return roles.stream()
                .map(RoleEntity::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** Identifiants des groupes auxquels l'utilisateur appartient. */
    @Transient
    public Set<Long> getGroupeIds() {
        if (groupes == null) {
            return new HashSet<>();
        }
        return groupes.stream()
                .map(GroupeEntity::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Permissions effectives (à plat) sous forme de chaînes : permissions des rôles
     * effectifs, plus les permissions <b>dérivées</b> de l'état de l'utilisateur
     * ({@link #rattacheACompte} ⇒ {@link PermissionEnum#TRESORERIE_READ}).
     *
     * <p>Source unique de vérité : {@link #getAuthorities()} en découle, ainsi que
     * le tableau {@code permissions} renvoyé au frontend.</p>
     */
    @Transient
    public Set<String> getPermissionNames() {
        Set<String> permissions = getEffectiveRoles().stream()
                .filter(role -> role.getPermissions() != null)
                .flatMap(role -> role.getPermissions().stream())
                .map(PermissionEnum::name)
                .collect(Collectors.toCollection(TreeSet::new));
        if (rattacheACompte) {
            permissions.add(PermissionEnum.TRESORERIE_READ.name());
        }
        return permissions;
    }

    /**
     * Rôle « principal » conservé pour rétro-compatibilité avec l'ancien champ
     * {@code role} (mono-rôle) exposé au frontend. Renvoie le premier rôle effectif.
     */
    @Transient
    public String getPrimaryRoleName() {
        return getRoleNames().stream().findFirst().orElse(null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String role : getRoleNames()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String permission : getPermissionNames()) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
