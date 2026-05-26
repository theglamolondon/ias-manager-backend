package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LigneFactureEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LigneFactureRepository extends JpaRepository<LigneFactureEntity, Long> {

    Page<LigneFactureEntity> findByFactureId(Long factureId, Pageable pageable);

    List<LigneFactureEntity> findByFactureId(Long factureId);

    /**
     * Lignes de facture dont l'extraRef matche l'un des codeMission donnés,
     * restreintes aux factures de type MISSION. Le filtre sur le type est
     * essentiel car extraRef est un champ générique multi-usages (cf.
     * FactureRepository.findByLigneExtraRef pour le détail). Utilisé pour
     * construire l'historique de facturation d'un véhicule à partir de ses
     * missions.
     */
    @Query("SELECT lf FROM LigneFactureEntity lf " +
            "WHERE lf.extraRef IN :extraRefs " +
            "AND lf.facture.type = net.ivoireautoservice.ias_manager.enums.FactureTypeEnum.MISSION")
    List<LigneFactureEntity> findByExtraRefInForMission(@Param("extraRefs") List<String> extraRefs);
}