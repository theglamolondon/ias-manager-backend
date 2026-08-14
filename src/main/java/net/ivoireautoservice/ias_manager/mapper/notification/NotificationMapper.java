package net.ivoireautoservice.ias_manager.mapper.notification;

import net.ivoireautoservice.ias_manager.dto.core.notification.Notification;
import net.ivoireautoservice.ias_manager.entity.notification.NotificationEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
	Notification toDto(NotificationEntity entity);
	List<Notification> toDtoList(List<NotificationEntity> entities);
}
