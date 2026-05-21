package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.PieceJointe;
import net.ivoireautoservice.ias_manager.entity.PieceJointeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PieceJointeMapper {

    @Mapping(source = "media.id", target = "mediaId")
    @Mapping(source = "media.originalFilename", target = "originalFilename")
    @Mapping(source = "media.contentType", target = "contentType")
    @Mapping(source = "media.size", target = "size")
    PieceJointe toDto(PieceJointeEntity entity);

    List<PieceJointe> toDtoList(List<PieceJointeEntity> entities);
}
