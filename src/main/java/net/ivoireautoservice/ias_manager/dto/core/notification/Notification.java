package net.ivoireautoservice.ias_manager.dto.core.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.ivoireautoservice.ias_manager.enums.notification.TypeNotificationEnum;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {

	private Long id;
	private TypeNotificationEnum type;
	private String titre;
	private String message;
	private String lien;
	private boolean lu;
	private LocalDateTime createdAt;
}
