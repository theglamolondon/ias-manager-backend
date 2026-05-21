package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.EntreeProduit;
import net.ivoireautoservice.ias_manager.dto.request.EntreeProduitRequest;
import net.ivoireautoservice.ias_manager.entity.EntreeProduitEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EntreeProduitMapper {

    @Mapping(source = "produit.id", target = "produitId")
    @Mapping(source = "produit.designation", target = "produitDesignation")
    @Mapping(source = "livraisonFournisseur.id", target = "livraisonFournisseurId")
    @Mapping(source = "ligneBonCommande.id", target = "ligneBonCommandeId")
    EntreeProduit toDto(EntreeProduitEntity entity);

    List<EntreeProduit> toDtoList(List<EntreeProduitEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "livraisonFournisseur", ignore = true)
    @Mapping(target = "ligneBonCommande", ignore = true)
    EntreeProduitEntity toEntity(EntreeProduitRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "produit", ignore = true)
    @Mapping(target = "livraisonFournisseur", ignore = true)
    @Mapping(target = "ligneBonCommande", ignore = true)
    void updateEntity(EntreeProduitRequest request, @MappingTarget EntreeProduitEntity entity);
}
