package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.LignePieceComptable;
import net.ivoireautoservice.ias_manager.dto.request.LignePieceComptableRequest;
import net.ivoireautoservice.ias_manager.entity.LignePieceComptableEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LignePieceComptableMapper {

    @Mapping(source = "pieceComptable.id", target = "pieceComptableId")
    @Mapping(source = "produit.id", target = "produitId")
    @Mapping(source = "produit.designation", target = "produitDesignation")
    LignePieceComptable toDto(LignePieceComptableEntity entity);

    List<LignePieceComptable> toDtoList(List<LignePieceComptableEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pieceComptable", ignore = true)
    @Mapping(target = "produit", ignore = true)
    LignePieceComptableEntity toEntity(LignePieceComptableRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pieceComptable", ignore = true)
    @Mapping(target = "produit", ignore = true)
    void updateEntity(LignePieceComptableRequest request, @MappingTarget LignePieceComptableEntity entity);
}
