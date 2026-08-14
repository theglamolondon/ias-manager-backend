package net.ivoireautoservice.ias_manager.services.notification;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.enums.notification.TypeNotificationEnum;
import net.ivoireautoservice.ias_manager.event.MissionCreatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transforme les événements de domaine en notifications.
 *
 * <p>{@code AFTER_COMMIT} : la notification n'est créée que si la transaction
 * métier a réellement commité, et son échec ne fait jamais échouer l'opération
 * d'origine. {@code REQUIRES_NEW} est indispensable ici : après commit, la
 * transaction d'origine est terminée, il en faut une nouvelle pour écrire.</p>
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

	private final NotificationService notificationService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onMissionCreated(MissionCreatedEvent event) {
		String message = event.clientNom() != null
				? String.format("La mission %s a été créée pour %s (véhicule %s).",
						event.codeMission(), event.clientNom(), event.immatriculation())
				: String.format("La mission %s a été créée (véhicule %s).",
						event.codeMission(), event.immatriculation());
		notificationService.notifierParPermission(
				PermissionEnum.MISSION_READ,
				TypeNotificationEnum.MISSION_CREEE,
				"Nouvelle mission " + event.codeMission(),
				message,
				"/missions/" + event.missionId(),
				null);
	}
}
