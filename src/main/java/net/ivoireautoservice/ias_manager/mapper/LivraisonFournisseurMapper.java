package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseur;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseurSummary;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LivraisonFournisseurMapper {

    @Mapping(source = "facture.id", target = "factureId")
    @Mapping(source = "facture.numProforma", target = "factureNumProforma")
    @Mapping(source = "bonCommande.id", target = "bonCommandeId")
    @Mapping(source = "bonCommande.numero", target = "bonCommandeNumero")
    @Mapping(source = "bonCommande.statut", target = "bonCommandeStatut")
    @Mapping(source = "bonCommande.partenaire.raisonSociale", target = "partenaireRaisonSociale")
    @Mapping(target = "entrees", ignore = true)
    LivraisonFournisseur toDto(LivraisonFournisseurEntity entity);

    @Mapping(source = "facture.id", target = "factureId")
    @Mapping(source = "facture.numProforma", target = "factureNumProforma")
    @Mapping(source = "bonCommande.id", target = "bonCommandeId")
    @Mapping(source = "bonCommande.numero", target = "bonCommandeNumero")
    @Mapping(source = "bonCommande.statut", target = "bonCommandeStatut")
    @Mapping(source = "bonCommande.partenaire.raisonSociale", target = "partenaireRaisonSociale")
    @Mapping(target = "montantHtEstime", ignore = true)
    LivraisonFournisseurSummary toSummary(LivraisonFournisseurEntity entity);

    List<LivraisonFournisseur> toDtoList(List<LivraisonFournisseurEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "bonCommande", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateValidation", ignore = true)
    @Mapping(target = "dateAnnulation", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LivraisonFournisseurEntity toEntity(LivraisonFournisseurRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "bonCommande", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "dateValidation", ignore = true)
    @Mapping(target = "dateAnnulation", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(LivraisonFournisseurRequest request, @MappingTarget LivraisonFournisseurEntity entity);
}
