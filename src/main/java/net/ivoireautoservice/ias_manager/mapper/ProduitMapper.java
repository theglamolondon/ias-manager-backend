package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Produit;
import net.ivoireautoservice.ias_manager.dto.request.ProduitRequest;
import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = MediaMapper.class)
public interface ProduitMapper {

    @Mapping(source = "famille.id", target = "familleId")
    @Mapping(source = "famille.libelle", target = "familleLibelle")
    Produit toDto(ProduitEntity entity);

    List<Produit> toDtoList(List<ProduitEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "famille", ignore = true)
    @Mapping(target = "image", ignore = true)
    ProduitEntity toEntity(ProduitRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "famille", ignore = true)
    @Mapping(target = "image", ignore = true)
    void updateEntity(ProduitRequest request, @MappingTarget ProduitEntity entity);
}
