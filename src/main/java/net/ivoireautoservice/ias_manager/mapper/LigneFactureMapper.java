package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.LigneFacture;
import net.ivoireautoservice.ias_manager.dto.request.LigneFactureRequest;
import net.ivoireautoservice.ias_manager.entity.LigneFactureEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LigneFactureMapper {

    @Mapping(source = "facture.id", target = "factureId")
    @Mapping(source = "produit.id", target = "produitId")
    @Mapping(source = "produit.designation", target = "produitDesignation")
    LigneFacture toDto(LigneFactureEntity entity);

    List<LigneFacture> toDtoList(List<LigneFactureEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "produit", ignore = true)
    LigneFactureEntity toEntity(LigneFactureRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "produit", ignore = true)
    void updateEntity(LigneFactureRequest request, @MappingTarget LigneFactureEntity entity);
}