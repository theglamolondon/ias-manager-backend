package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.LigneBonCommande;
import net.ivoireautoservice.ias_manager.dto.request.LigneBonCommandeRequest;
import net.ivoireautoservice.ias_manager.entity.LigneBonCommandeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LigneBonCommandeMapper {

    @Mapping(source = "bonCommande.id", target = "bonCommandeId")
    @Mapping(source = "produit.id", target = "produitId")
    @Mapping(source = "produit.designation", target = "produitDesignation")
    LigneBonCommande toDto(LigneBonCommandeEntity entity);

    List<LigneBonCommande> toDtoList(List<LigneBonCommandeEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "qteLivree", ignore = true)
    @Mapping(target = "bonCommande", ignore = true)
    @Mapping(target = "produit", ignore = true)
    LigneBonCommandeEntity toEntity(LigneBonCommandeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "qteLivree", ignore = true)
    @Mapping(target = "bonCommande", ignore = true)
    @Mapping(target = "produit", ignore = true)
    void updateEntity(LigneBonCommandeRequest request, @MappingTarget LigneBonCommandeEntity entity);
}
