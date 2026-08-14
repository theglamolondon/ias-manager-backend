package net.ivoireautoservice.ias_manager.repository.notification;

import net.ivoireautoservice.ias_manager.entity.notification.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

	Page<NotificationEntity> findByDestinataireId(Long destinataireId, Pageable pageable);

	long countByDestinataireIdAndLuFalse(Long destinataireId);

	Optional<NotificationEntity> findByIdAndDestinataireId(Long id, Long destinataireId);

	boolean existsByDestinataireIdAndCleDedoublonnage(Long destinataireId, String cleDedoublonnage);

	@Modifying
	@Query("UPDATE NotificationEntity n SET n.lu = true WHERE n.destinataire.id = :destinataireId AND n.lu = false")
	int marquerToutLu(@Param("destinataireId") Long destinataireId);

	/** Purge des notifications déjà lues plus anciennes que la date donnée (rétention). */
	@Modifying
	@Query("DELETE FROM NotificationEntity n WHERE n.lu = true AND n.createdAt < :avant")
	int supprimerLuesAvant(@Param("avant") LocalDateTime avant);
}
