package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Site;
import net.ivoireautoservice.ias_manager.dto.request.SiteRequest;
import net.ivoireautoservice.ias_manager.entity.SiteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SiteMapper {

	Site toDto(SiteEntity entity);

	List<Site> toDtoList(List<SiteEntity> entities);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	SiteEntity toEntity(SiteRequest request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void updateEntity(SiteRequest request, @MappingTarget SiteEntity entity);
}
