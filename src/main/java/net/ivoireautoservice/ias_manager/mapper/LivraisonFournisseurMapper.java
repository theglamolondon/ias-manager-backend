package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseur;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LivraisonFournisseurMapper {

    LivraisonFournisseur toDto(LivraisonFournisseurEntity entity);

    List<LivraisonFournisseur> toDtoList(List<LivraisonFournisseurEntity> entities);

    @Mapping(target = "id", ignore = true)
    LivraisonFournisseurEntity toEntity(LivraisonFournisseurRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(LivraisonFournisseurRequest request, @MappingTarget LivraisonFournisseurEntity entity);
}
