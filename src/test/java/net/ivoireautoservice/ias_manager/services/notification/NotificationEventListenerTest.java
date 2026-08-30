package net.ivoireautoservice.ias_manager.services.notification;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.enums.notification.TypeNotificationEnum;
import net.ivoireautoservice.ias_manager.event.MissionCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener — traduction des événements métier en notifications")
class NotificationEventListenerTest {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private NotificationEventListener listener;

	@Test
	@DisplayName("une mission avec client mentionne le client dans le message")
	void avecClient() {
		listener.onMissionCreated(new MissionCreatedEvent(1L, "2026-001", "AB-123-CD", "Total CI"));

		ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
		verify(notificationService).notifierParPermission(
				eq(PermissionEnum.MISSION_READ),
				eq(TypeNotificationEnum.MISSION_CREEE),
				eq("Nouvelle mission 2026-001"),
				message.capture(),
				eq("/missions/1"),
				any());
		assertThat(message.getValue()).contains("Total CI").contains("AB-123-CD").contains("2026-001");
	}

	@Test
	@DisplayName("une mission sans client produit un message sans mention de client")
	void sansClient() {
		listener.onMissionCreated(new MissionCreatedEvent(2L, "2026-002", "EF-456-GH", null));

		ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
		verify(notificationService).notifierParPermission(
				any(), any(), any(), message.capture(), eq("/missions/2"), any());
		assertThat(message.getValue()).contains("EF-456-GH").doesNotContain("pour ");
	}

	@Test
	@DisplayName("la notification de création de mission n'est pas dédoublonnée")
	void sansDedoublonnage() {
		listener.onMissionCreated(new MissionCreatedEvent(1L, "2026-001", "AB-123-CD", "Total CI"));

		verify(notificationService).notifierParPermission(
				any(), any(), any(), any(), any(), eq((String) null));
	}
}
