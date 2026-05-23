package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.BonCommande;
import net.ivoireautoservice.ias_manager.dto.request.BonCommandeRequest;
import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BonCommandeMapper {

    @Mapping(source = "partenaire.id", target = "partenaireId")
    @Mapping(source = "partenaire.raisonSociale", target = "partenaireRaisonSociale")
    @Mapping(source = "createdBy.id", target = "createdById")
    @Mapping(source = "createdBy", target = "createdByNom", qualifiedByName = "utilisateurNomComplet")
    @Mapping(target = "items", ignore = true)
    BonCommande toDto(BonCommandeEntity entity);

    List<BonCommande> toDtoList(List<BonCommandeEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numero", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    BonCommandeEntity toEntity(BonCommandeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numero", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(BonCommandeRequest request, @MappingTarget BonCommandeEntity entity);

    @Named("utilisateurNomComplet")
    default String utilisateurNomComplet(Utilisateur u) {
        if (u == null) return null;
        String prenom = u.getPrenom() != null ? u.getPrenom() : "";
        String nom = u.getNom() != null ? u.getNom() : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? null : full;
    }
}
