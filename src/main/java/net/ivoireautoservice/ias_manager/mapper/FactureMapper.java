package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Facture;
import net.ivoireautoservice.ias_manager.dto.request.FactureRequest;
import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FactureMapper {

    @Mapping(source = "partenaire.id", target = "partenaireId")
    @Mapping(source = "partenaire.raisonSociale", target = "partenaireRaisonSociale")
    @Mapping(target = "items", ignore = true)
    Facture toDto(FactureEntity entity);

    List<Facture> toDtoList(List<FactureEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    @Mapping(target = "statut", ignore = true)
    FactureEntity toEntity(FactureRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    @Mapping(target = "statut", ignore = true)
    void updateEntity(FactureRequest request, @MappingTarget FactureEntity entity);
}