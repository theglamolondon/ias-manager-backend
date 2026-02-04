package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Mission;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.entity.MissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MissionMapper {

	@Mapping(source = "vehicule.id", target = "vehiculeId")
	@Mapping(source = "vehicule.immatriculation", target = "vehiculeImmatriculation")
	@Mapping(source = "chauffeur.id", target = "chauffeurId")
	@Mapping(source = "chauffeur.numeroPermis", target = "chauffeurNumeroPermis")
	@Mapping(target = "depenses", ignore = true)
	@Mapping(target = "medias", ignore = true)
	Mission toDto(MissionEntity entity);

	List<Mission> toDtoList(List<MissionEntity> entities);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "vehicule", ignore = true)
	@Mapping(target = "chauffeur", ignore = true)
	MissionEntity toEntity(MissionRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "vehicule", ignore = true)
	@Mapping(target = "chauffeur", ignore = true)
	void updateEntity(MissionRequest request, @MappingTarget MissionEntity entity);
}
