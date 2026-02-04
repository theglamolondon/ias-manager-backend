package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.DepenseMission;
import net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest;
import net.ivoireautoservice.ias_manager.entity.DepenseMissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DepenseMissionMapper {

	@Mapping(source = "typeDepense.id", target = "typeDepenseId")
	@Mapping(source = "typeDepense.libelle", target = "typeDepenseLibelle")
	DepenseMission toDto(DepenseMissionEntity entity);

	List<DepenseMission> toDtoList(List<DepenseMissionEntity> entities);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "mission", ignore = true)
	@Mapping(target = "typeDepense", ignore = true)
	DepenseMissionEntity toEntity(DepenseMissionRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "mission", ignore = true)
	@Mapping(target = "typeDepense", ignore = true)
	void updateEntity(DepenseMissionRequest request, @MappingTarget DepenseMissionEntity entity);
}
