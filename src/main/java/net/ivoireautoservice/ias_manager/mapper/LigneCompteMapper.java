package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.LigneCompte;
import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LigneCompteMapper {

    @Mapping(source = "utilisateur.id", target = "utilisateurId")
    @Mapping(source = "utilisateur.nom", target = "utilisateurNom")
    @Mapping(source = "utilisateur.prenom", target = "utilisateurPrenom")
    @Mapping(source = "compte.id", target = "compteId")
    @Mapping(source = "compte.intitule", target = "compteIntitule")
    @Mapping(source = "facture.id", target = "factureId")
    LigneCompte toDto(LigneCompteEntity entity);

    List<LigneCompte> toDtoList(List<LigneCompteEntity> entities);
}
