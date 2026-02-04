package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.TypeStatutPieceComptable;
import net.ivoireautoservice.ias_manager.dto.request.TypeStatutPieceComptableRequest;
import net.ivoireautoservice.ias_manager.entity.TypeStatutPieceComptableEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TypeStatutPieceComptableMapper {

    TypeStatutPieceComptable toDto(TypeStatutPieceComptableEntity entity);

    List<TypeStatutPieceComptable> toDtoList(List<TypeStatutPieceComptableEntity> entities);

    @Mapping(target = "id", ignore = true)
    TypeStatutPieceComptableEntity toEntity(TypeStatutPieceComptableRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(TypeStatutPieceComptableRequest request, @MappingTarget TypeStatutPieceComptableEntity entity);
}
