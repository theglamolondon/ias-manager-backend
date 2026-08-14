package net.ivoireautoservice.ias_manager.services.notification;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.enums.notification.TypeNotificationEnum;
import net.ivoireautoservice.ias_manager.repository.VehiculeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Contrôle quotidien des échéances documentaires des véhicules
 * (visite technique, assurance, patente, cartes de stationnement/transport).
 *
 * <p>Un seuil franchi (J-30, J-15, J-7, expiré) génère une notification vers les
 * détenteurs de {@code VEHICULE_READ}. La clé de dédoublonnage — véhicule +
 * document + date d'échéance + seuil — garantit qu'un même seuil n'est notifié
 * qu'une fois, et que le renouvellement du document (nouvelle date) relance un
 * cycle d'alertes propre.</p>
 */
@Component
@RequiredArgsConstructor
public class DocumentExpirationScheduler {

	private static final Logger log = LoggerFactory.getLogger(DocumentExpirationScheduler.class);

	/** Seuils d'alerte en jours restants, du plus urgent au plus lointain. */
	private static final int[] SEUILS_JOURS = {0, 7, 15, 30};
	private static final int RETENTION_LUES_JOURS = 60;
	private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final VehiculeRepository vehiculeRepository;
	private final NotificationService notificationService;

	@Scheduled(cron = "0 0 7 * * *")
	public void verifierExpirations() {
		List<VehiculeEntity> vehicules = vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME);
		log.info("Contrôle des échéances documentaires : {} véhicules", vehicules.size());
		for (VehiculeEntity vehicule : vehicules) {
			try {
				verifierVehicule(vehicule);
			} catch (Exception e) {
				log.error("Échec du contrôle d'expiration pour le véhicule {}", vehicule.getId(), e);
			}
		}
		int purgees = notificationService.purgerAnciennesLues(RETENTION_LUES_JOURS);
		if (purgees > 0) {
			log.info("{} notifications lues purgées (rétention {} jours)", purgees, RETENTION_LUES_JOURS);
		}
	}

	private void verifierVehicule(VehiculeEntity vehicule) {
		verifierDocument(vehicule, "Visite technique", "VISITE", vehicule.getFinValiditeVisite());
		verifierDocument(vehicule, "Assurance", "ASSURANCE", vehicule.getFinValiditeAssurance());
		verifierDocument(vehicule, "Patente", "PATENTE", vehicule.getFinValiditePatente());
		verifierDocument(vehicule, "Carte de stationnement", "CARTE_STATIONNEMENT", vehicule.getFinValiditeCarteStationnement());
		verifierDocument(vehicule, "Carte de transport", "CARTE_TRANSPORT", vehicule.getFinValiditeCarteTransport());
	}

	private void verifierDocument(VehiculeEntity vehicule, String libelle, String codeDocument, LocalDate finValidite) {
		if (finValidite == null) {
			return;
		}
		long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), finValidite);
		for (int seuil : SEUILS_JOURS) {
			if (joursRestants <= seuil) {
				notifier(vehicule, libelle, codeDocument, finValidite, joursRestants, seuil);
				return;
			}
		}
	}

	private void notifier(VehiculeEntity vehicule, String libelle, String codeDocument,
			LocalDate finValidite, long joursRestants, int seuil) {
		String cle = String.format("DOC_EXPIRATION:%d:%s:%s:J-%d",
				vehicule.getId(), codeDocument, finValidite, seuil);
		String titre;
		String message;
		if (joursRestants < 0) {
			titre = String.format("%s expirée — %s", libelle, vehicule.getImmatriculation());
			message = String.format("%s du véhicule %s : expirée depuis le %s.",
					libelle, vehicule.getImmatriculation(), finValidite.format(FORMAT_DATE));
		} else if (joursRestants == 0) {
			titre = String.format("%s expire aujourd'hui — %s", libelle, vehicule.getImmatriculation());
			message = String.format("%s du véhicule %s : expire aujourd'hui (%s).",
					libelle, vehicule.getImmatriculation(), finValidite.format(FORMAT_DATE));
		} else {
			titre = String.format("%s expire dans %d jour%s — %s",
					libelle, joursRestants, joursRestants > 1 ? "s" : "", vehicule.getImmatriculation());
			message = String.format("%s du véhicule %s : expire le %s.",
					libelle, vehicule.getImmatriculation(), finValidite.format(FORMAT_DATE));
		}
		String lien = vehicule.getNumChassis() != null ? "/vehicules/" + vehicule.getNumChassis() : null;
		notificationService.notifierParPermission(
				PermissionEnum.VEHICULE_READ,
				TypeNotificationEnum.DOCUMENT_EXPIRATION,
				titre, message, lien, cle);
	}
}
