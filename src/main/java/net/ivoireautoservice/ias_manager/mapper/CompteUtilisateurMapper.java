package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.CompteUtilisateur;
import net.ivoireautoservice.ias_manager.entity.CompteUtilisateurEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompteUtilisateurMapper {

	@Mapping(source = "utilisateur.id", target = "utilisateurId")
	@Mapping(source = "utilisateur.nom", target = "utilisateurNom")
	@Mapping(source = "utilisateur.prenom", target = "utilisateurPrenom")
	@Mapping(source = "utilisateur.email", target = "utilisateurEmail")
	CompteUtilisateur toDto(CompteUtilisateurEntity entity);

	List<CompteUtilisateur> toDtoList(List<CompteUtilisateurEntity> entities);
}
