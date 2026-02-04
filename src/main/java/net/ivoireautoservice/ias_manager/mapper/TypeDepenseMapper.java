package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.TypeDepense;
import net.ivoireautoservice.ias_manager.dto.request.TypeDepenseRequest;
import net.ivoireautoservice.ias_manager.entity.TypeDepenseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TypeDepenseMapper {

	TypeDepense toDto(TypeDepenseEntity entity);

	List<TypeDepense> toDtoList(List<TypeDepenseEntity> entities);

	@Mapping(target = "id", ignore = true)
	TypeDepenseEntity toEntity(TypeDepenseRequest request);

	@Mapping(target = "id", ignore = true)
	void updateEntity(TypeDepenseRequest request, @MappingTarget TypeDepenseEntity entity);
}
