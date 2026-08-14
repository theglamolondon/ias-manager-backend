package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);

    /**
     * Utilisateurs détenant une permission, via leurs rôles directs ou les rôles
     * hérités de leurs groupes (même dérivation que {@link Utilisateur#getEffectiveRoles()},
     * mais côté base pour ne pas charger tous les utilisateurs).
     */
    @Query("""
            SELECT DISTINCT u FROM Utilisateur u
            LEFT JOIN u.roles r
            LEFT JOIN u.groupes g
            LEFT JOIN g.roles gr
            WHERE :permission MEMBER OF r.permissions
               OR :permission MEMBER OF gr.permissions
            """)
    List<Utilisateur> findAllByPermission(@Param("permission") PermissionEnum permission);
}
