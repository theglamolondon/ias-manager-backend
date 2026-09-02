package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByNom(String nom);

    boolean existsByNom(String nom);

    /**
     * Valeurs distinctes présentes dans {@code role_permissions}.
     *
     * <p>Requête native volontaire : une valeur qui n'existe plus dans
     * {@code PermissionEnum} n'est pas mappable, la lire en JPQL lèverait une
     * {@code IllegalArgumentException}.</p>
     */
    @Query(value = "SELECT DISTINCT permission FROM role_permissions", nativeQuery = true)
    List<String> findPermissionsDistinctes();

    /**
     * Supprime les attributions portant exactement les valeurs passées.
     *
     * <p>La liste est <b>énumérée explicitement</b> par l'appelant (jamais un
     * {@code NOT IN} sur le catalogue) : ainsi, même si le paramètre était mal
     * transmis, la requête ne peut pas effacer des permissions valides.</p>
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM role_permissions WHERE permission IN (:noms)", nativeQuery = true)
    int supprimerPermissions(@Param("noms") Collection<String> noms);
}
