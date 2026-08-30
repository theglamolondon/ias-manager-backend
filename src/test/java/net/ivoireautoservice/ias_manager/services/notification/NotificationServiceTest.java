package net.ivoireautoservice.ias_manager.services.notification;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.dto.core.notification.Notification;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.entity.notification.NotificationEntity;
import net.ivoireautoservice.ias_manager.enums.notification.TypeNotificationEnum;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.notification.NotificationMapper;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import net.ivoireautoservice.ias_manager.repository.notification.NotificationRepository;
import net.ivoireautoservice.ias_manager.services.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService — diffusion ciblée et consultation scopée")
class NotificationServiceTest {

	@Mock private NotificationRepository notificationRepository;
	@Mock private UserRepository userRepository;
	@Mock private NotificationMapper notificationMapper;
	@Mock private SecurityService securityService;

	@InjectMocks
	private NotificationService service;

	private Utilisateur connecte;

	@BeforeEach
	void setUp() {
		connecte = Utilisateur.builder().id(7L).email("agent@ias.ci").build();
	}

	@Nested
	@DisplayName("Diffusion par permission")
	class Diffusion {

		@Test
		@DisplayName("une notification est créée pour chaque détenteur de la permission ciblée")
		void fanOut() {
			Utilisateur a = Utilisateur.builder().id(1L).build();
			Utilisateur b = Utilisateur.builder().id(2L).build();
			when(userRepository.findAllByPermission(PermissionEnum.MISSION_READ)).thenReturn(List.of(a, b));

			service.notifierParPermission(PermissionEnum.MISSION_READ, TypeNotificationEnum.MISSION_CREEE,
					"Titre", "Message", "/missions/1", null);

			verify(notificationRepository, times(2)).save(any(NotificationEntity.class));
		}

		@Test
		@DisplayName("le contenu de la notification est intégralement repris")
		void contenu() {
			Utilisateur destinataire = Utilisateur.builder().id(1L).build();
			when(userRepository.findAllByPermission(PermissionEnum.VEHICULE_READ))
					.thenReturn(List.of(destinataire));

			service.notifierParPermission(PermissionEnum.VEHICULE_READ,
					TypeNotificationEnum.DOCUMENT_EXPIRATION,
					"Assurance expirée", "Détail", "/vehicules/VF1", "CLE-1");

			ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
			verify(notificationRepository).save(captor.capture());
			NotificationEntity entity = captor.getValue();
			assertThat(entity.getDestinataire()).isSameAs(destinataire);
			assertThat(entity.getType()).isEqualTo(TypeNotificationEnum.DOCUMENT_EXPIRATION);
			assertThat(entity.getTitre()).isEqualTo("Assurance expirée");
			assertThat(entity.getMessage()).isEqualTo("Détail");
			assertThat(entity.getLien()).isEqualTo("/vehicules/VF1");
			assertThat(entity.getCleDedoublonnage()).isEqualTo("CLE-1");
			assertThat(entity.isLu()).isFalse();
		}

		@Test
		@DisplayName("un destinataire ayant déjà reçu la même clé de dédoublonnage est ignoré")
		void dedoublonnage() {
			Utilisateur a = Utilisateur.builder().id(1L).build();
			Utilisateur b = Utilisateur.builder().id(2L).build();
			when(userRepository.findAllByPermission(any())).thenReturn(List.of(a, b));
			when(notificationRepository.existsByDestinataireIdAndCleDedoublonnage(1L, "CLE-1")).thenReturn(true);
			when(notificationRepository.existsByDestinataireIdAndCleDedoublonnage(2L, "CLE-1")).thenReturn(false);

			service.notifierParPermission(PermissionEnum.VEHICULE_READ,
					TypeNotificationEnum.DOCUMENT_EXPIRATION, "T", "M", null, "CLE-1");

			verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
		}

		@Test
		@DisplayName("sans clé de dédoublonnage, aucun contrôle d'unicité n'est effectué")
		void sansCle() {
			when(userRepository.findAllByPermission(any()))
					.thenReturn(List.of(Utilisateur.builder().id(1L).build()));

			service.notifierParPermission(PermissionEnum.MISSION_READ,
					TypeNotificationEnum.MISSION_CREEE, "T", "M", null, null);

			verify(notificationRepository, never()).existsByDestinataireIdAndCleDedoublonnage(any(), any());
			verify(notificationRepository).save(any(NotificationEntity.class));
		}

		@Test
		@DisplayName("aucun destinataire : aucune notification créée")
		void aucunDestinataire() {
			when(userRepository.findAllByPermission(any())).thenReturn(List.of());

			service.notifierParPermission(PermissionEnum.MISSION_READ,
					TypeNotificationEnum.MISSION_CREEE, "T", "M", null, null);

			verify(notificationRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("Consultation scopée sur l'utilisateur connecté")
	class Consultation {

		@Test
		@DisplayName("le compteur de non-lues porte sur l'utilisateur connecté")
		void compteurNonLues() {
			when(securityService.getUtilisateurConnecte()).thenReturn(connecte);
			when(notificationRepository.countByDestinataireIdAndLuFalse(7L)).thenReturn(3L);

			assertThat(service.countMesNotificationsNonLues()).isEqualTo(3L);
		}

		@Test
		@DisplayName("marquer lu ne fonctionne que sur ses propres notifications")
		void marquerLuScopé() {
			NotificationEntity entity = NotificationEntity.builder().id(1L).lu(false).build();
			when(securityService.getUtilisateurConnecte()).thenReturn(connecte);
			when(notificationRepository.findByIdAndDestinataireId(1L, 7L)).thenReturn(Optional.of(entity));
			when(notificationRepository.save(entity)).thenReturn(entity);
			when(notificationMapper.toDto(entity)).thenReturn(new Notification());

			service.marquerLu(1L);

			assertThat(entity.isLu()).isTrue();
		}

		@Test
		@DisplayName("la notification d'un autre utilisateur est traitée comme introuvable")
		void notificationEtrangere() {
			when(securityService.getUtilisateurConnecte()).thenReturn(connecte);
			when(notificationRepository.findByIdAndDestinataireId(1L, 7L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.marquerLu(1L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Notification avec l'id 1");
		}

		@Test
		@DisplayName("supprimer une notification étrangère est refusé")
		void suppressionEtrangere() {
			when(securityService.getUtilisateurConnecte()).thenReturn(connecte);
			when(notificationRepository.findByIdAndDestinataireId(1L, 7L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.supprimer(1L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(notificationRepository, never()).delete(any());
		}

		@Test
		@DisplayName("marquer tout lu porte sur les seules notifications de l'utilisateur connecté")
		void marquerToutLu() {
			when(securityService.getUtilisateurConnecte()).thenReturn(connecte);
			when(notificationRepository.marquerToutLu(7L)).thenReturn(5);

			assertThat(service.marquerToutLu()).isEqualTo(5);
		}
	}

	@Nested
	@DisplayName("Purge")
	class Purge {

		@Test
		@DisplayName("la purge cible les notifications lues plus anciennes que la rétention")
		void purge() {
			when(notificationRepository.supprimerLuesAvant(any(LocalDateTime.class))).thenReturn(12);

			assertThat(service.purgerAnciennesLues(60)).isEqualTo(12);

			ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
			verify(notificationRepository).supprimerLuesAvant(captor.capture());
			assertThat(captor.getValue()).isBefore(LocalDateTime.now().minusDays(59));
		}
	}
}
