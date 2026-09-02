package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.CompteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Toutes les lectures « périmètre trésorerie » partagent le même paramètre
 * {@code userId} : <b>{@code null} = accès total</b> (trésorier en chef,
 * cf. {@code TRESORERIE_ADMIN}), sinon seuls les comptes rattachés à
 * l'utilisateur — affectation explicite (compte_utilisateurs) ou rôle de
 * manager du compte — sont visibles. Le filtrage reste en SQL pour que la
 * pagination et les agrégats restent justes.
 */
public interface CompteRepository extends JpaRepository<CompteEntity, Long> {

    String PERIMETRE = """
            (:userId IS NULL
             OR c.manager.id = :userId
             OR EXISTS (SELECT 1 FROM CompteUtilisateurEntity cu
                        WHERE cu.compte = c AND cu.utilisateur.id = :userId))
            """;

    Optional<CompteEntity> findByNumero(String numero);

    @Query("SELECT c FROM CompteEntity c LEFT JOIN c.manager m WHERE " + PERIMETRE + """
            AND (:keyword IS NULL
                 OR LOWER(c.numero) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(c.intitule) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(m.nom) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(m.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<CompteEntity> search(@Param("keyword") String keyword,
                              @Param("userId") Long userId,
                              Pageable pageable);

    @Query("SELECT c FROM CompteEntity c WHERE c.id = :id AND " + PERIMETRE)
    Optional<CompteEntity> findVisibleById(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT c FROM CompteEntity c WHERE c.numero = :numero AND " + PERIMETRE)
    Optional<CompteEntity> findVisibleByNumero(@Param("numero") String numero, @Param("userId") Long userId);

    /**
     * Statistiques des comptes du périmètre en <b>une seule</b> requête. Ligne unique :
     * [nombre, balance totale, somme des soldes positifs, somme des soldes négatifs].
     */
    @Query("SELECT COUNT(c), COALESCE(SUM(c.balance), 0), "
            + "COALESCE(SUM(CASE WHEN c.balance > 0 THEN c.balance ELSE 0 END), 0), "
            + "COALESCE(SUM(CASE WHEN c.balance < 0 THEN c.balance ELSE 0 END), 0) "
            + "FROM CompteEntity c WHERE " + PERIMETRE)
    List<Object[]> statistiques(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM CompteEntity c")
    long sumBalance();

    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM CompteEntity c WHERE c.balance > 0")
    long sumSoldesPositifs();

    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM CompteEntity c WHERE c.balance < 0")
    long sumSoldesNegatifs();
}
