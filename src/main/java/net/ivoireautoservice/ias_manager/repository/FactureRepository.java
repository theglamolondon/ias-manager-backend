package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<FactureEntity, Long> {

    Optional<FactureEntity> findByNumProforma(String numProforma);

    Optional<FactureEntity> findByNumFacture(String numFacture);

    Optional<FactureEntity> findFirstByFactureOrigineId(Long factureOrigineId);

    List<FactureEntity> findByNumProformaIn(List<String> numProformas);

    /**
     * Toutes les factures de type MISSION qui contiennent au moins une ligne
     * dont l'extraRef correspond au codeMission donné. Permet de retrouver
     * les factures émises pour une mission donnée (mission classique
     * auto-facturée ou facturation groupée multi-missions).
     *
     * Le filtre sur f.type = MISSION est essentiel : extraRef est un champ
     * générique multi-usages (rempli aussi par les lignes de factures
     * fournisseur depuis LigneBonCommande.extraRef, texte libre). Sans ce
     * filtre, un extraRef de BC qui coïnciderait par hasard avec un
     * codeMission ferait remonter une facture fournisseur comme étant liée
     * à la mission, ce qui est faux.
     */
    @Query("SELECT DISTINCT f FROM FactureEntity f JOIN LigneFactureEntity lf ON lf.facture = f " +
            "WHERE lf.extraRef = :codeMission " +
            "AND f.type = net.ivoireautoservice.ias_manager.enums.FactureTypeEnum.MISSION " +
            "ORDER BY f.createdAt DESC")
    List<FactureEntity> findByLigneExtraRef(@Param("codeMission") String codeMission);

    /**
     * Pour une liste de codeMission, renvoie les couples (extraRef, FactureEntity)
     * en se limitant aux factures de type MISSION (cf. javadoc de
     * findByLigneExtraRef pour la raison du filtre sur le type).
     * Utilisé pour construire en bulk la map codeMission → facture liée
     * (table missions).
     */
    @Query("SELECT DISTINCT lf.extraRef, lf.facture FROM LigneFactureEntity lf " +
            "WHERE lf.extraRef IN :codes " +
            "AND lf.facture.type = net.ivoireautoservice.ias_manager.enums.FactureTypeEnum.MISSION")
    List<Object[]> findFacturesByLigneExtraRefIn(@Param("codes") List<String> codes);

    /**
     * Retourne le plus grand suffixe numérique des proformas dont le numProforma commence par le préfixe donné.
     * Utilisé pour la génération auto du numéro proforma au format {prefix}{seq}.
     */
    @Query("SELECT MAX(CAST(SUBSTRING(f.numProforma, LENGTH(:prefix) + 1) AS int)) " +
            "FROM FactureEntity f WHERE f.numProforma LIKE CONCAT(:prefix, '%')")
    Integer findMaxNumProformaSuffix(@Param("prefix") String prefix);

    Page<FactureEntity> findByPartenaireIsClientTrue(Pageable pageable);

    Page<FactureEntity> findByPartenaireIsFournisseurTrue(Pageable pageable);

    Page<FactureEntity> findByFactureClient(Boolean factureClient, Pageable pageable);

    @Query("SELECT f FROM FactureEntity f WHERE f.statut IN :statuts " +
            "AND f.factureClient = :factureClient " +
            "AND f.type <> net.ivoireautoservice.ias_manager.enums.FactureTypeEnum.MISSION " +
            "AND NOT EXISTS (SELECT lc FROM LivraisonClientEntity lc WHERE lc.facture = f) " +
            "AND NOT EXISTS (SELECT lf FROM LivraisonFournisseurEntity lf WHERE lf.facture = f)")
    Page<FactureEntity> findFacturesSansLivraison(
            @Param("statuts") List<FactureStatusEnum> statuts,
            @Param("factureClient") Boolean factureClient,
            Pageable pageable);

    @Query("SELECT f FROM FactureEntity f WHERE f.statut IN :statuts " +
            "AND f.type <> net.ivoireautoservice.ias_manager.enums.FactureTypeEnum.MISSION " +
            "AND NOT EXISTS (SELECT lc FROM LivraisonClientEntity lc WHERE lc.facture = f) " +
            "AND NOT EXISTS (SELECT lf FROM LivraisonFournisseurEntity lf WHERE lf.facture = f)")
    Page<FactureEntity> findFacturesSansLivraison(
            @Param("statuts") List<FactureStatusEnum> statuts,
            Pageable pageable);

    @Query("SELECT f FROM FactureEntity f LEFT JOIN f.partenaire p " +
            "WHERE (f.factureClient = :factureClient OR :factureClient IS NULL) " +
            "AND (LOWER(f.numFacture) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(f.numProforma) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.raisonSociale) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY f.createdAt DESC")
    Page<FactureEntity> searchByKeyword(
            @Param("keyword") String keyword,
            @Param("factureClient") Boolean factureClient,
            Pageable pageable);

    /**
     * Recherche paginée multi-critères des factures. Tous les critères sont
     * optionnels et se cumulent (ET logique) : un critère à null (ou vide pour
     * les chaînes) n'applique aucune restriction.
     *
     * <p>Deux subtilités volontaires :</p>
     * <ul>
     *   <li>{@code numFacture} filtre aussi sur {@code numProforma} : une facture
     *   au statut PROFORMA n'a pas encore de numéro de facture définitif, un
     *   filtre strict sur la seule colonne {@code numFacture} serait donc aveugle
     *   sur une grande partie du stock.</li>
     *   <li>{@code codeMission} est borné aux factures de type MISSION, pour la
     *   même raison que {@link #findByLigneExtraRef(String)} : {@code extraRef}
     *   est un champ générique multi-usages, également alimenté en texte libre
     *   depuis {@code LigneBonCommande.extraRef}. Sans cette garde, un extraRef
     *   de bon de commande qui coïnciderait avec un codeMission ferait remonter
     *   une facture fournisseur comme liée à la mission.</li>
     * </ul>
     */
    @Query("SELECT f FROM FactureEntity f LEFT JOIN f.partenaire p " +
            "WHERE (:factureClient IS NULL OR f.factureClient = :factureClient) " +
            "AND (:partenaireId IS NULL OR p.id = :partenaireId) " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin) " +
            "AND (COALESCE(:keyword, '') = '' " +
            "    OR LOWER(f.numFacture) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(f.numProforma) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(p.raisonSociale) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (COALESCE(:numFacture, '') = '' " +
            "    OR LOWER(f.numFacture) LIKE LOWER(CONCAT('%', :numFacture, '%')) " +
            "    OR LOWER(f.numProforma) LIKE LOWER(CONCAT('%', :numFacture, '%'))) " +
            "AND (COALESCE(:codeMission, '') = '' " +
            "    OR (f.type = net.ivoireautoservice.ias_manager.enums.FactureTypeEnum.MISSION " +
            "        AND EXISTS (SELECT lf FROM LigneFactureEntity lf " +
            "                    WHERE lf.facture = f " +
            "                    AND LOWER(lf.extraRef) LIKE LOWER(CONCAT('%', :codeMission, '%')))))")
    Page<FactureEntity> findFiltered(
            @Param("keyword") String keyword,
            @Param("factureClient") Boolean factureClient,
            @Param("partenaireId") Long partenaireId,
            @Param("numFacture") String numFacture,
            @Param("codeMission") String codeMission,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin,
            Pageable pageable);

    @Query("SELECT MONTH(f.createdAt), SUM(f.montantTtc) FROM FactureEntity f " +
            "WHERE f.factureClient = true AND f.statut = :statut " +
            "AND YEAR(f.createdAt) = :annee " +
            "GROUP BY MONTH(f.createdAt) ORDER BY MONTH(f.createdAt)")
    List<Object[]> revenusMensuelsFacturesClient(
            @Param("statut") FactureStatusEnum statut,
            @Param("annee") int annee);

    @Query("SELECT MONTH(f.createdAt), COALESCE(SUM(f.montantTtc), 0) FROM FactureEntity f " +
            "WHERE f.factureClient = false AND f.statut = :statut " +
            "AND YEAR(f.createdAt) = :annee " +
            "GROUP BY MONTH(f.createdAt) ORDER BY MONTH(f.createdAt)")
    List<Object[]> depensesMensuellesFournisseur(
            @Param("statut") FactureStatusEnum statut,
            @Param("annee") int annee);

    long countByFactureClientAndCreatedAtBetween(Boolean factureClient, java.time.LocalDateTime debut, java.time.LocalDateTime fin);

    long countByFactureClientAndStatutAndCreatedAtBetween(Boolean factureClient, FactureStatusEnum statut, java.time.LocalDateTime debut, java.time.LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(f.montantTtc), 0) FROM FactureEntity f WHERE f.factureClient = :factureClient AND f.statut = :statut AND f.createdAt BETWEEN :debut AND :fin")
    long sumMontantPayeByFactureClient(@Param("factureClient") Boolean factureClient, @Param("statut") FactureStatusEnum statut, @Param("debut") java.time.LocalDateTime debut, @Param("fin") java.time.LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(f.montantTtc), 0) FROM FactureEntity f WHERE f.factureClient = :factureClient AND f.statut != net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.ANNULEE AND f.createdAt BETWEEN :debut AND :fin")
    long sumMontantTotalByFactureClient(@Param("factureClient") Boolean factureClient, @Param("debut") java.time.LocalDateTime debut, @Param("fin") java.time.LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(f.montantTtc), 0) FROM FactureEntity f WHERE f.factureClient = :factureClient AND f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) AND f.createdAt BETWEEN :debut AND :fin")
    long sumMontantImpayeByFactureClient(@Param("factureClient") Boolean factureClient, @Param("debut") java.time.LocalDateTime debut, @Param("fin") java.time.LocalDateTime fin);

    /**
     * Agrégats (nombre + montant TTC) par statut, pour les KPI des listes de
     * factures. Les critères sont volontairement identiques à ceux de
     * {@link #findFiltered} — mêmes prédicats, même sémantique — pour que les
     * cartes affichées au-dessus d'une liste correspondent exactement à son
     * contenu. Tous sont optionnels : des bornes de dates nulles ne bornent pas
     * la période, les KPI portent alors sur l'intégralité de l'historique.
     * La borne {@code fin} est exclusive.
     */
    @Query("SELECT new net.ivoireautoservice.ias_manager.dto.core.StatutAgregat(f.statut, COUNT(f), COALESCE(SUM(f.montantTtc), 0)) " +
            "FROM FactureEntity f LEFT JOIN f.partenaire p " +
            "WHERE f.factureClient = :factureClient " +
            "AND (:partenaireId IS NULL OR p.id = :partenaireId) " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin) " +
            "AND (COALESCE(:keyword, '') = '' " +
            "    OR LOWER(f.numFacture) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(f.numProforma) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "    OR LOWER(p.raisonSociale) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (COALESCE(:numFacture, '') = '' " +
            "    OR LOWER(f.numFacture) LIKE LOWER(CONCAT('%', :numFacture, '%')) " +
            "    OR LOWER(f.numProforma) LIKE LOWER(CONCAT('%', :numFacture, '%'))) " +
            "AND (COALESCE(:codeMission, '') = '' " +
            "    OR (f.type = net.ivoireautoservice.ias_manager.enums.FactureTypeEnum.MISSION " +
            "        AND EXISTS (SELECT lf FROM LigneFactureEntity lf " +
            "                    WHERE lf.facture = f " +
            "                    AND LOWER(lf.extraRef) LIKE LOWER(CONCAT('%', :codeMission, '%'))))) " +
            "GROUP BY f.statut")
    List<net.ivoireautoservice.ias_manager.dto.core.StatutAgregat> statsParStatut(
            @Param("factureClient") Boolean factureClient,
            @Param("partenaireId") Long partenaireId,
            @Param("keyword") String keyword,
            @Param("numFacture") String numFacture,
            @Param("codeMission") String codeMission,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(f), COALESCE(SUM(f.montantTtc), 0) FROM FactureEntity f " +
            "WHERE f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, " +
            "net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE)")
    List<Object[]> countAndSumFacturesImpayees();

    // ── Rapport financier : requêtes optimisées ──

    /**
     * Agrégats financiers en une seule requête :
     * [0] chiffreAffaire (total TTC hors annulées)
     * [1] montantEncaisse (total TTC PAYEE)
     * [2] nombreEncaissees
     * [3] montantAVenir (non échues, statut PROFORMA/FACTUREE, validite > today)
     * [4] nombreAVenir
     * [5] montantEnRetard (échues, statut PROFORMA/FACTUREE, validite <= today)
     * [6] nombreEnRetard
     * [7] nombreTotal
     * [8] chiffreAffaireHt (total HT hors annulées/brouillon)
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN f.statut NOT IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.ANNULEE, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.BROUILLON) THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.statut = net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PAYEE THEN f.montantTtc ELSE 0 END), 0), " +
            "SUM(CASE WHEN f.statut = net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PAYEE THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) AND (f.validite IS NULL OR f.validite > :aujourdhui) THEN f.montantTtc ELSE 0 END), 0), " +
            "SUM(CASE WHEN f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) AND (f.validite IS NULL OR f.validite > :aujourdhui) THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) AND f.validite IS NOT NULL AND f.validite <= :aujourdhui THEN f.montantTtc ELSE 0 END), 0), " +
            "SUM(CASE WHEN f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) AND f.validite IS NOT NULL AND f.validite <= :aujourdhui THEN 1 ELSE 0 END), " +
            "COUNT(f), " +
            "COALESCE(SUM(CASE WHEN f.statut NOT IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.ANNULEE, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.BROUILLON) THEN f.montantHt ELSE 0 END), 0) " +
            "FROM FactureEntity f WHERE f.factureClient = true " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin)")
    List<Object[]> rapportFinancierKpis(
            @Param("aujourdhui") LocalDate aujourdhui,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /**
     * Balance âgée : montants par tranche d'ancienneté pour factures clients impayées.
     * Retourne [nonEchu, echu0a30, echu31a60, echu61a90, echuPlus90]
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN f.validite IS NULL OR f.validite > :aujourdhui THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.validite IS NOT NULL AND f.validite <= :aujourdhui AND f.validite > :j30 THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.validite IS NOT NULL AND f.validite <= :j30 AND f.validite > :j60 THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.validite IS NOT NULL AND f.validite <= :j60 AND f.validite > :j90 THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.validite IS NOT NULL AND f.validite <= :j90 THEN f.montantTtc ELSE 0 END), 0) " +
            "FROM FactureEntity f WHERE f.factureClient = true " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin)")
    List<Object[]> rapportBalanceAgee(
            @Param("aujourdhui") LocalDate aujourdhui,
            @Param("j30") LocalDate j30,
            @Param("j60") LocalDate j60,
            @Param("j90") LocalDate j90,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /**
     * Rentrées de fonds mensuelles : chiffre d'affaire et encaissement par mois.
     * Retourne [annee, mois, chiffreAffaire, encaissement]
     */
    @Query("SELECT YEAR(f.createdAt), MONTH(f.createdAt), " +
            "COALESCE(SUM(CASE WHEN f.statut NOT IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.ANNULEE, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.BROUILLON) THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.statut = net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PAYEE THEN f.montantTtc ELSE 0 END), 0) " +
            "FROM FactureEntity f WHERE f.factureClient = true " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin) " +
            "GROUP BY YEAR(f.createdAt), MONTH(f.createdAt) " +
            "ORDER BY YEAR(f.createdAt), MONTH(f.createdAt)")
    List<Object[]> rapportRentreesFondsMensuelles(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /**
     * DSO mensuel : créances et CA par mois pour calculer le DSO.
     * Retourne [annee, mois, totalCreances (impayées à la fin du mois), chiffreAffaire]
     */
    @Query("SELECT YEAR(f.createdAt), MONTH(f.createdAt), " +
            "COALESCE(SUM(CASE WHEN f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.statut NOT IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.ANNULEE, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.BROUILLON) THEN f.montantTtc ELSE 0 END), 0) " +
            "FROM FactureEntity f WHERE f.factureClient = true " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin) " +
            "GROUP BY YEAR(f.createdAt), MONTH(f.createdAt) " +
            "ORDER BY YEAR(f.createdAt), MONTH(f.createdAt)")
    List<Object[]> rapportDsoMensuel(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /**
     * 5 plus anciennes factures échues (client, impayées, validité dépassée).
     */
    @Query("SELECT f.id, COALESCE(f.numFacture, f.numProforma), p.raisonSociale, f.validite, f.montantTtc " +
            "FROM FactureEntity f LEFT JOIN f.partenaire p " +
            "WHERE f.factureClient = true " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) " +
            "AND f.validite IS NOT NULL AND f.validite <= :aujourdhui " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin) " +
            "ORDER BY f.validite ASC")
    List<Object[]> rapportFacturesEchuesTop5(
            @Param("aujourdhui") LocalDate aujourdhui,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin,
            Pageable pageable);

    /**
     * Top 5 clients par solde impayé.
     * Retourne [partenaireId, raisonSociale, totalImpaye]
     */
    @Query("SELECT p.id, p.raisonSociale, COALESCE(SUM(f.montantTtc), 0) " +
            "FROM FactureEntity f JOIN f.partenaire p " +
            "WHERE f.factureClient = true " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin) " +
            "GROUP BY p.id, p.raisonSociale " +
            "ORDER BY SUM(f.montantTtc) DESC")
    List<Object[]> rapportTopClientsImpayes(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin,
            Pageable pageable);

    /**
     * Montant à relancer par tranche.
     * Retourne [totalMontant, totalNombre, relance1Montant, relance1Nombre, relance2Montant, relance2Nombre, relance3Montant, relance3Nombre]
     */
    @Query("SELECT " +
            "COALESCE(SUM(f.montantTtc), 0), COUNT(f), " +
            "COALESCE(SUM(CASE WHEN f.validite > :j30 THEN f.montantTtc ELSE 0 END), 0), " +
            "SUM(CASE WHEN f.validite > :j30 THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN f.validite <= :j30 AND f.validite > :j60 THEN f.montantTtc ELSE 0 END), 0), " +
            "SUM(CASE WHEN f.validite <= :j30 AND f.validite > :j60 THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN f.validite <= :j60 THEN f.montantTtc ELSE 0 END), 0), " +
            "SUM(CASE WHEN f.validite <= :j60 THEN 1 ELSE 0 END) " +
            "FROM FactureEntity f WHERE f.factureClient = true " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) " +
            "AND f.validite IS NOT NULL AND f.validite <= :aujourdhui " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin)")
    List<Object[]> rapportMontantRelance(
            @Param("aujourdhui") LocalDate aujourdhui,
            @Param("j30") LocalDate j30,
            @Param("j60") LocalDate j60,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    // ── Rapport financier : volet fournisseur, avoirs et TVA ──

    /**
     * Agrégats fournisseur (dettes / comptes à payer) en une seule requête :
     * [0] totalFacture (TTC hors annulées/brouillon)
     * [1] totalPaye (TTC PAYEE)
     * [2] totalDu (TTC PROFORMA/FACTUREE)
     * [3] totalDuEchu (PROFORMA/FACTUREE, validite échue)
     * [4] totalDuAVenir (PROFORMA/FACTUREE, non échue ou sans validité)
     * [5] nombre (PROFORMA/FACTUREE)
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN f.statut NOT IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.ANNULEE, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.BROUILLON) THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.statut = net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PAYEE THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) AND f.validite IS NOT NULL AND f.validite <= :aujourdhui THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) AND (f.validite IS NULL OR f.validite > :aujourdhui) THEN f.montantTtc ELSE 0 END), 0), " +
            "SUM(CASE WHEN f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) THEN 1 ELSE 0 END) " +
            "FROM FactureEntity f WHERE f.factureClient = false " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin)")
    List<Object[]> rapportFournisseurKpis(
            @Param("aujourdhui") LocalDate aujourdhui,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /**
     * Balance âgée fournisseur : montants par tranche d'ancienneté pour factures
     * fournisseur impayées. Retourne [nonEchu, echu0a30, echu31a60, echu61a90, echuPlus90].
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN f.validite IS NULL OR f.validite > :aujourdhui THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.validite IS NOT NULL AND f.validite <= :aujourdhui AND f.validite > :j30 THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.validite IS NOT NULL AND f.validite <= :j30 AND f.validite > :j60 THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.validite IS NOT NULL AND f.validite <= :j60 AND f.validite > :j90 THEN f.montantTtc ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN f.validite IS NOT NULL AND f.validite <= :j90 THEN f.montantTtc ELSE 0 END), 0) " +
            "FROM FactureEntity f WHERE f.factureClient = false " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND f.statut IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.PROFORMA, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.FACTUREE) " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin)")
    List<Object[]> rapportBalanceAgeeFournisseur(
            @Param("aujourdhui") LocalDate aujourdhui,
            @Param("j30") LocalDate j30,
            @Param("j60") LocalDate j60,
            @Param("j90") LocalDate j90,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /**
     * Avoirs clients émis sur la période. Retourne [nombre, montantTtc].
     */
    @Query("SELECT COUNT(f), COALESCE(SUM(f.montantTtc), 0) FROM FactureEntity f " +
            "WHERE f.factureClient = true " +
            "AND f.nature = net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND f.statut <> net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.ANNULEE " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin)")
    List<Object[]> rapportAvoirs(
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);

    /**
     * TVA agrégée sur la période pour un sens donné (client = collectée, fournisseur = déductible).
     * Assiette : montantHt * tva / 100. Exclut avoirs, annulées et brouillons.
     */
    @Query("SELECT COALESCE(SUM(f.montantHt * f.tva / 100), 0) FROM FactureEntity f " +
            "WHERE f.factureClient = :factureClient " +
            "AND f.nature <> net.ivoireautoservice.ias_manager.enums.FactureNatureEnum.AVOIR " +
            "AND f.statut NOT IN (net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.ANNULEE, net.ivoireautoservice.ias_manager.enums.FactureStatusEnum.BROUILLON) " +
            "AND f.tva IS NOT NULL AND f.montantHt IS NOT NULL " +
            "AND (:debut IS NULL OR f.createdAt >= :debut) " +
            "AND (:fin IS NULL OR f.createdAt < :fin)")
    Double rapportSumTva(
            @Param("factureClient") Boolean factureClient,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin);
}