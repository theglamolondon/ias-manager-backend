package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.SortieProduit;
import net.ivoireautoservice.ias_manager.dto.request.SortieProduitRequest;
import net.ivoireautoservice.ias_manager.entity.SortieProduitEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SortieProduitMapper {

    @Mapping(source = "livraisonClient.id", target = "livraisonClientId")
    @Mapping(source = "produit.id", target = "produitId")
    @Mapping(source = "produit.designation", target = "produitDesignation")
    SortieProduit toDto(SortieProduitEntity entity);

    List<SortieProduit> toDtoList(List<SortieProduitEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "livraisonClient", ignore = true)
    @Mapping(target = "produit", ignore = true)
    SortieProduitEntity toEntity(SortieProduitRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "livraisonClient", ignore = true)
    @Mapping(target = "produit", ignore = true)
    void updateEntity(SortieProduitRequest request, @MappingTarget SortieProduitEntity entity);
}
