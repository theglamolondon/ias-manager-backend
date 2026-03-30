package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.DocumentVehicule;
import net.ivoireautoservice.ias_manager.entity.DocumentVehiculeEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MediaMapper.class})
public interface DocumentVehiculeMapper {
	DocumentVehicule toDto(DocumentVehiculeEntity entity);
	List<DocumentVehicule> toDtoList(List<DocumentVehiculeEntity> entities);
}
