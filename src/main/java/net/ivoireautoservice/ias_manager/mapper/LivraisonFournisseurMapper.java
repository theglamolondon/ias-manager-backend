package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseur;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseurSummary;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LivraisonFournisseurMapper {

    @Mapping(source = "facture.id", target = "factureId")
    @Mapping(source = "facture.numProforma", target = "factureNumProforma")
    @Mapping(target = "entrees", ignore = true)
    LivraisonFournisseur toDto(LivraisonFournisseurEntity entity);

    @Mapping(source = "facture.id", target = "factureId")
    @Mapping(source = "facture.numProforma", target = "factureNumProforma")
    LivraisonFournisseurSummary toSummary(LivraisonFournisseurEntity entity);

    List<LivraisonFournisseur> toDtoList(List<LivraisonFournisseurEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "facture", ignore = true)
    LivraisonFournisseurEntity toEntity(LivraisonFournisseurRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "facture", ignore = true)
    void updateEntity(LivraisonFournisseurRequest request, @MappingTarget LivraisonFournisseurEntity entity);
}
