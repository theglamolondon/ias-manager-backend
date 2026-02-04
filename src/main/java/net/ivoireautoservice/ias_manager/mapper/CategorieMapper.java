package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Categorie;
import net.ivoireautoservice.ias_manager.dto.request.CategorieRequest;
import net.ivoireautoservice.ias_manager.entity.CategorieEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategorieMapper {

    Categorie toDto(CategorieEntity entity);

    List<Categorie> toDtoList(List<CategorieEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "typeVehicules", ignore = true)
    CategorieEntity toEntity(CategorieRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "typeVehicules", ignore = true)
    void updateEntity(CategorieRequest request, @MappingTarget CategorieEntity entity);
}
