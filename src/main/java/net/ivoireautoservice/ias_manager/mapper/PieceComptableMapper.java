package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.PieceComptable;
import net.ivoireautoservice.ias_manager.dto.request.PieceComptableRequest;
import net.ivoireautoservice.ias_manager.entity.PieceComptableEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PieceComptableMapper {

    @Mapping(source = "typeStatut.id", target = "typeStatutId")
    @Mapping(source = "typeStatut.libelle", target = "typeStatutLibelle")
    @Mapping(source = "partenaire.id", target = "partenaireId")
    @Mapping(source = "partenaire.raisonSociale", target = "partenaireRaisonSociale")
    PieceComptable toDto(PieceComptableEntity entity);

    List<PieceComptable> toDtoList(List<PieceComptableEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "typeStatut", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    PieceComptableEntity toEntity(PieceComptableRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "typeStatut", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    void updateEntity(PieceComptableRequest request, @MappingTarget PieceComptableEntity entity);
}
