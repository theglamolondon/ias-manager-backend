package net.ivoireautoservice.ias_manager.mapper;

import net.ivoireautoservice.ias_manager.dto.core.Facture;
import net.ivoireautoservice.ias_manager.dto.request.FactureRequest;
import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = PartenaireMapper.class)
public interface FactureMapper {

    @Mapping(target = "items", ignore = true)
    @Mapping(target = "livraison", ignore = true)
    @Mapping(source = "factureOrigine.id", target = "factureOrigineId")
    @Mapping(source = "factureOrigine", target = "factureOrigineNumero", qualifiedByName = "origineNumero")
    @Mapping(source = "createdBy.id", target = "createdById")
    @Mapping(source = "createdBy", target = "createdByNom", qualifiedByName = "factureUtilisateurNom")
    Facture toDto(FactureEntity entity);

    List<Facture> toDtoList(List<FactureEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "nature", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "factureOrigine", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    FactureEntity toEntity(FactureRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "partenaire", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "nature", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "factureOrigine", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(FactureRequest request, @MappingTarget FactureEntity entity);

    @Named("origineNumero")
    default String origineNumero(FactureEntity origine) {
        if (origine == null) return null;
        return origine.getNumFacture() != null ? origine.getNumFacture() : origine.getNumProforma();
    }

    @Named("factureUtilisateurNom")
    default String factureUtilisateurNom(Utilisateur u) {
        if (u == null) return null;
        String prenom = u.getPrenom() != null ? u.getPrenom() : "";
        String nom = u.getNom() != null ? u.getNom() : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? null : full;
    }
}
