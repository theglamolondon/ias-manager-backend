package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

/**
 * Rôle : paquet nommé de {@link PermissionEnum}, géré par un administrateur.
 *
 * <p>Niveau intermédiaire du modèle RBAC :
 * {@code Permission ⊂ Role ⊂ Groupe}. Un rôle peut être attribué directement à un
 * utilisateur, ou via un {@link GroupeEntity}.</p>
 */
@Entity
@Table(name = "roles")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = "permissions")
@EqualsAndHashCode(callSuper = false, of = "id")
public class RoleEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    private String libelle;

    @Column(length = 500)
    private String description;

    /**
     * Rôle système (seedé au démarrage). Protégé contre la suppression et le
     * renommage afin de garantir un socle d'accès stable (ex. ADMIN).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean systemRole = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    @BatchSize(size = 30)
    @Builder.Default
    private Set<PermissionEnum> permissions = new HashSet<>();
}
