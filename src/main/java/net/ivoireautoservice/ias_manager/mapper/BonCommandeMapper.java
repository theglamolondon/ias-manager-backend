package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.BonCommande;
import net.ivoireautoservice.ias_manager.dto.request.BonCommandeRequest;
import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BonCommandeMapper {

    @Mapping(source = "partenaire.id", target = "partenaireId")
    @Mapping(source = "partenaire.raisonSociale", target = "partenaireRaisonSociale")
    BonCommande toDto(BonCommandeEntity entity);

    List<BonCommande> toDtoList(List<BonCommandeEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    BonCommandeEntity toEntity(BonCommandeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    void updateEntity(BonCommandeRequest request, @MappingTarget BonCommandeEntity entity);
}
