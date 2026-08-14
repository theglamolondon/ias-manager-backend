package net.ivoireautoservice.ias_manager.services.notification;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.notification.Notification;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.entity.notification.NotificationEntity;
import net.ivoireautoservice.ias_manager.enums.notification.TypeNotificationEnum;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.notification.NotificationMapper;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import net.ivoireautoservice.ias_manager.repository.notification.NotificationRepository;
import net.ivoireautoservice.ias_manager.services.SecurityService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Diffusion et consultation des notifications in-app.
 *
 * <p>Le RBAC intervient en amont (qui <i>reçoit</i> : ciblage par permission) ;
 * la consultation est toujours scopée sur l'utilisateur connecté, aucune
 * permission dédiée n'est nécessaire.</p>
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final NotificationMapper notificationMapper;
	private final SecurityService securityService;

	// ==================== DIFFUSION ====================

	/**
	 * Crée une notification pour chaque utilisateur détenant la permission cible
	 * (fan-out à l'écriture). Les destinataires ayant déjà reçu la même
	 * {@code cleDedoublonnage} sont ignorés.
	 */
	@Transactional
	public void notifierParPermission(PermissionEnum permissionCible, TypeNotificationEnum type,
			String titre, String message, String lien, String cleDedoublonnage) {
		List<Utilisateur> destinataires = userRepository.findAllByPermission(permissionCible);
		for (Utilisateur destinataire : destinataires) {
			if (cleDedoublonnage != null && notificationRepository
					.existsByDestinataireIdAndCleDedoublonnage(destinataire.getId(), cleDedoublonnage)) {
				continue;
			}
			notificationRepository.save(NotificationEntity.builder()
					.destinataire(destinataire)
					.type(type)
					.titre(titre)
					.message(message)
					.lien(lien)
					.cleDedoublonnage(cleDedoublonnage)
					.build());
		}
	}

	// ==================== CONSULTATION (scopée utilisateur connecté) ====================

	@Transactional(readOnly = true)
	public PagedResponse<Notification> getMesNotifications(Pageable pageable) {
		Long userId = securityService.getUtilisateurConnecte().getId();
		return PagedResponse.of(notificationRepository.findByDestinataireId(userId, pageable)
				.map(notificationMapper::toDto));
	}

	@Transactional(readOnly = true)
	public long countMesNotificationsNonLues() {
		Long userId = securityService.getUtilisateurConnecte().getId();
		return notificationRepository.countByDestinataireIdAndLuFalse(userId);
	}

	@Transactional
	public Notification marquerLu(Long id) {
		NotificationEntity entity = getMaNotification(id);
		entity.setLu(true);
		return notificationMapper.toDto(notificationRepository.save(entity));
	}

	@Transactional
	public int marquerToutLu() {
		Long userId = securityService.getUtilisateurConnecte().getId();
		return notificationRepository.marquerToutLu(userId);
	}

	@Transactional
	public void supprimer(Long id) {
		notificationRepository.delete(getMaNotification(id));
	}

	/** Purge les notifications lues plus anciennes que {@code joursRetention} jours. */
	@Transactional
	public int purgerAnciennesLues(int joursRetention) {
		return notificationRepository.supprimerLuesAvant(LocalDateTime.now().minusDays(joursRetention));
	}

	/** Charge une notification en vérifiant qu'elle appartient bien à l'utilisateur connecté. */
	private NotificationEntity getMaNotification(Long id) {
		Long userId = securityService.getUtilisateurConnecte().getId();
		return notificationRepository.findByIdAndDestinataireId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification", id));
	}
}
