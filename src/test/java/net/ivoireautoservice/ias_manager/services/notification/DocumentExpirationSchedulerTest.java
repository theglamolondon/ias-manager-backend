package net.ivoireautoservice.ias_manager.services.notification;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.enums.notification.TypeNotificationEnum;
import net.ivoireautoservice.ias_manager.repository.VehiculeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentExpirationScheduler — alertes d'échéances documentaires")
class DocumentExpirationSchedulerTest {

	@Mock
	private VehiculeRepository vehiculeRepository;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private DocumentExpirationScheduler scheduler;

	private static VehiculeEntity vehicule() {
		return VehiculeEntity.builder()
				.id(1L).immatriculation("AB-123-CD").numChassis("VF123")
				.statut(VehiculeStatusEnum.DISPONIBLE).build();
	}

	private ArgumentCaptor<String> lancerEtCapturerTitres(VehiculeEntity vehicule) {
		when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of(vehicule));
		scheduler.verifierExpirations();
		ArgumentCaptor<String> titres = ArgumentCaptor.forClass(String.class);
		verify(notificationService, org.mockito.Mockito.atLeastOnce()).notifierParPermission(
				eq(PermissionEnum.VEHICULE_READ), eq(TypeNotificationEnum.DOCUMENT_EXPIRATION),
				titres.capture(), anyString(), any(), anyString());
		return titres;
	}

	@Nested
	@DisplayName("Franchissement des seuils")
	class Seuils {

		@Test
		@DisplayName("un document expirant dans 30 jours déclenche une alerte")
		void seuilTrente() {
			VehiculeEntity vehicule = vehicule();
			vehicule.setFinValiditeAssurance(LocalDate.now().plusDays(30));

			assertThat(lancerEtCapturerTitres(vehicule).getValue())
					.contains("Assurance").contains("expire dans 30 jours");
		}

		@Test
		@DisplayName("un document expirant dans 31 jours n'alerte pas encore")
		void auDelaDuSeuil() {
			VehiculeEntity vehicule = vehicule();
			vehicule.setFinValiditeAssurance(LocalDate.now().plusDays(31));
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of(vehicule));

			scheduler.verifierExpirations();

			verify(notificationService, never()).notifierParPermission(
					any(), any(), anyString(), anyString(), any(), any());
		}

		@Test
		@DisplayName("un document expirant aujourd'hui produit un message dédié")
		void expireAujourdhui() {
			VehiculeEntity vehicule = vehicule();
			vehicule.setFinValiditeVisite(LocalDate.now());

			assertThat(lancerEtCapturerTitres(vehicule).getValue())
					.contains("Visite technique").contains("expire aujourd'hui");
		}

		@Test
		@DisplayName("un document déjà expiré produit un message au passé")
		void dejaExpire() {
			VehiculeEntity vehicule = vehicule();
			vehicule.setFinValiditePatente(LocalDate.now().minusDays(5));

			assertThat(lancerEtCapturerTitres(vehicule).getValue())
					.contains("Patente").contains("expirée");
		}

		@Test
		@DisplayName("un seul seuil (le plus urgent) est notifié par document")
		void unSeulSeuilParDocument() {
			VehiculeEntity vehicule = vehicule();
			vehicule.setFinValiditeAssurance(LocalDate.now().plusDays(3));
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of(vehicule));

			scheduler.verifierExpirations();

			verify(notificationService, times(1)).notifierParPermission(
					any(), any(), anyString(), anyString(), any(), anyString());
		}

		@Test
		@DisplayName("un document sans date d'échéance est ignoré")
		void sansDate() {
			VehiculeEntity vehicule = vehicule();
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of(vehicule));

			scheduler.verifierExpirations();

			verify(notificationService, never()).notifierParPermission(
					any(), any(), anyString(), anyString(), any(), any());
		}

		@Test
		@DisplayName("les cinq documents suivis sont contrôlés indépendamment")
		void cinqDocuments() {
			VehiculeEntity vehicule = vehicule();
			LocalDate demain = LocalDate.now().plusDays(1);
			vehicule.setFinValiditeVisite(demain);
			vehicule.setFinValiditeAssurance(demain);
			vehicule.setFinValiditePatente(demain);
			vehicule.setFinValiditeCarteStationnement(demain);
			vehicule.setFinValiditeCarteTransport(demain);
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of(vehicule));

			scheduler.verifierExpirations();

			verify(notificationService, times(5)).notifierParPermission(
					any(), any(), anyString(), anyString(), any(), anyString());
		}
	}

	@Nested
	@DisplayName("Clé de dédoublonnage et lien")
	class CleEtLien {

		@Test
		@DisplayName("la clé combine véhicule, document, date d'échéance et seuil")
		void cle() {
			VehiculeEntity vehicule = vehicule();
			LocalDate echeance = LocalDate.now().plusDays(7);
			vehicule.setFinValiditeAssurance(echeance);
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of(vehicule));

			scheduler.verifierExpirations();

			ArgumentCaptor<String> cle = ArgumentCaptor.forClass(String.class);
			verify(notificationService).notifierParPermission(
					any(), any(), anyString(), anyString(), any(), cle.capture());
			assertThat(cle.getValue()).isEqualTo("DOC_EXPIRATION:1:ASSURANCE:" + echeance + ":J-7");
		}

		@Test
		@DisplayName("le lien pointe vers la fiche véhicule par numéro de chassis")
		void lien() {
			VehiculeEntity vehicule = vehicule();
			vehicule.setFinValiditeAssurance(LocalDate.now());
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of(vehicule));

			scheduler.verifierExpirations();

			ArgumentCaptor<String> lien = ArgumentCaptor.forClass(String.class);
			verify(notificationService).notifierParPermission(
					any(), any(), anyString(), anyString(), lien.capture(), anyString());
			assertThat(lien.getValue()).isEqualTo("/vehicules/VF123");
		}

		@Test
		@DisplayName("un véhicule sans numéro de chassis ne porte pas de lien")
		void sansChassis() {
			VehiculeEntity vehicule = vehicule();
			vehicule.setNumChassis(null);
			vehicule.setFinValiditeAssurance(LocalDate.now());
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of(vehicule));

			scheduler.verifierExpirations();

			ArgumentCaptor<String> lien = ArgumentCaptor.forClass(String.class);
			verify(notificationService).notifierParPermission(
					any(), any(), anyString(), anyString(), lien.capture(), anyString());
			assertThat(lien.getValue()).isNull();
		}
	}

	@Nested
	@DisplayName("Robustesse et purge")
	class RobustessePurge {

		@Test
		@DisplayName("l'échec sur un véhicule n'interrompt pas le contrôle des suivants")
		void echecIsole() {
			VehiculeEntity enErreur = vehicule();
			enErreur.setFinValiditeAssurance(LocalDate.now());
			VehiculeEntity suivant = VehiculeEntity.builder()
					.id(2L).immatriculation("EF-456-GH").numChassis("VF456")
					.finValiditeAssurance(LocalDate.now()).build();
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME))
					.thenReturn(List.of(enErreur, suivant));
			org.mockito.Mockito.doThrow(new RuntimeException("panne"))
					.doNothing()
					.when(notificationService).notifierParPermission(
							any(), any(), anyString(), anyString(), any(), anyString());

			scheduler.verifierExpirations();

			verify(notificationService, times(2)).notifierParPermission(
					any(), any(), anyString(), anyString(), any(), anyString());
		}

		@Test
		@DisplayName("la purge des notifications lues est déclenchée à chaque passage")
		void purge() {
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of());

			scheduler.verifierExpirations();

			verify(notificationService).purgerAnciennesLues(60);
		}

		@Test
		@DisplayName("les véhicules réformés sont exclus du contrôle")
		void vehiculesReformesExclus() {
			when(vehiculeRepository.findByStatutNot(VehiculeStatusEnum.REFORME)).thenReturn(List.of());

			scheduler.verifierExpirations();

			verify(vehiculeRepository).findByStatutNot(VehiculeStatusEnum.REFORME);
		}
	}
}
