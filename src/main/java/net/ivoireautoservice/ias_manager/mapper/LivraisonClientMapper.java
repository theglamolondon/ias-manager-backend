package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.LivraisonClient;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonClientSummary;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonClientRequest;
import net.ivoireautoservice.ias_manager.entity.LivraisonClientEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LivraisonClientMapper {

    @Mapping(source = "facture.id", target = "factureId")
    @Mapping(source = "facture.numProforma", target = "factureNumProforma")
    @Mapping(source = "facture.type", target = "factureType")
    @Mapping(source = "createdBy.id", target = "createdById")
    @Mapping(source = "createdBy", target = "createdByNom", qualifiedByName = "livraisonClientUtilisateurNom")
    @Mapping(target = "sorties", ignore = true)
    LivraisonClient toDto(LivraisonClientEntity entity);

    @Mapping(source = "facture.id", target = "factureId")
    @Mapping(source = "facture.numProforma", target = "factureNumProforma")
    LivraisonClientSummary toSummary(LivraisonClientEntity entity);

    List<LivraisonClient> toDtoList(List<LivraisonClientEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    LivraisonClientEntity toEntity(LivraisonClientRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(LivraisonClientRequest request, @MappingTarget LivraisonClientEntity entity);

    @Named("livraisonClientUtilisateurNom")
    default String livraisonClientUtilisateurNom(Utilisateur u) {
        if (u == null) return null;
        String prenom = u.getPrenom() != null ? u.getPrenom() : "";
        String nom = u.getNom() != null ? u.getNom() : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? null : full;
    }
}
