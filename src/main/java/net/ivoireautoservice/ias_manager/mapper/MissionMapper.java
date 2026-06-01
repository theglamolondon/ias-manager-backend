package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Mission;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.entity.MissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {VehiculeMapper.class, ChauffeurMapper.class, PartenaireMapper.class})
public interface MissionMapper {

	@Mapping(target = "depenses", ignore = true)
	@Mapping(target = "photos", ignore = true)
	@Mapping(target = "facture", ignore = true)
	Mission toDto(MissionEntity entity);

	List<Mission> toDtoList(List<MissionEntity> entities);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "codeMission", ignore = true)
	@Mapping(target = "vehicule", ignore = true)
	@Mapping(target = "chauffeur", ignore = true)
	@Mapping(target = "client", ignore = true)
	@Mapping(target = "dhmsAnnulation", ignore = true)
	@Mapping(target = "motifAnnulation", ignore = true)
	MissionEntity toEntity(MissionRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "codeMission", ignore = true)
	@Mapping(target = "vehicule", ignore = true)
	@Mapping(target = "chauffeur", ignore = true)
	@Mapping(target = "client", ignore = true)
	@Mapping(target = "dhmsAnnulation", ignore = true)
	@Mapping(target = "motifAnnulation", ignore = true)
	void updateEntity(MissionRequest request, @MappingTarget MissionEntity entity);
}
