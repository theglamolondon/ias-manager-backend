package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

/**
 * Groupe : paquet nommé de {@link RoleEntity}, géré par un administrateur.
 *
 * <p>Niveau le plus haut du modèle RBAC : un utilisateur ajouté à un groupe hérite
 * de tous les rôles du groupe (et donc de toutes leurs permissions).</p>
 */
@Entity
@Table(name = "groupes")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = "roles")
@EqualsAndHashCode(callSuper = false, of = "id")
public class GroupeEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(length = 500)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "groupe_roles",
            joinColumns = @JoinColumn(name = "groupe_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @BatchSize(size = 30)
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();
}
