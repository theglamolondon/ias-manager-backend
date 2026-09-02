package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByNom(String nom);

    boolean existsByNom(String nom);

    /**
     * Supprime de {@code role_permissions} toute valeur qui n'existe plus dans
     * {@code PermissionEnum} (permission renommée ou retirée du code).
     *
     * <p>Requête native volontaire : ces lignes ne sont plus mappables sur l'enum,
     * charger le {@code RoleEntity} correspondant lèverait une
     * {@code IllegalArgumentException} avant même qu'on puisse les nettoyer.</p>
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM role_permissions WHERE permission NOT IN (:noms)", nativeQuery = true)
    int purgerPermissionsInconnues(@Param("noms") Collection<String> noms);
}
