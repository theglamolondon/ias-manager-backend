package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Media;
import net.ivoireautoservice.ias_manager.entity.MediaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MediaMapper {

    @Mapping(target = "url", source = "id", qualifiedByName = "idToUrl")
    Media toDto(MediaEntity entity);

    List<Media> toDtoList(List<MediaEntity> entities);

    @Named("idToUrl")
    default String idToUrl(String id) {
        return "/api/resources/" + id;
    }
}
