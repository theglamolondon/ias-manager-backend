package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class StatistiqueService {

	private final VehiculeRepository vehiculeRepository;
	private final FactureRepository factureRepository;
	private final MissionRepository missionRepository;
	private final InterventionRepository interventionRepository;
	private final ChauffeurRepository chauffeurRepository;
	private final ProduitRepository produitRepository;
	private final LivraisonFournisseurRepository livraisonFournisseurRepository;
	private final LivraisonClientRepository livraisonClientRepository;
	private final CompteRepository compteRepository;
	private final EntreeProduitRepository entreeProduitRepository;
	private final SortieProduitRepository sortieProduitRepository;

	@Transactional(readOnly = true)
	public StatistiqueDashboard getDashboard(int annee) {
		return StatistiqueDashboard.builder()
				.vehiculesParStatut(getVehiculesParStatut())
				.vehiculesParCarburant(getVehiculesParCarburant())
				.revenusMensuels(getRevenusMensuels(annee))
				.build();
	}

	private List<VehiculeStatutStat> getVehiculesParStatut() {
		List<Object[]> resultats = vehiculeRepository.countGroupByStatut();

		long total = resultats.stream()
				.mapToLong(r -> ((Number) r[1]).longValue())
				.sum();

		Map<VehiculeStatusEnum, Long> countMap = resultats.stream()
				.collect(Collectors.toMap(
						r -> (VehiculeStatusEnum) r[0],
						r -> ((Number) r[1]).longValue()
				));

		return Arrays.stream(VehiculeStatusEnum.values())
				.map(statut -> {
					long nombre = countMap.getOrDefault(statut, 0L);
					double pourcentage = total > 0 ? Math.round(nombre * 10000.0 / total) / 100.0 : 0.0;
					return VehiculeStatutStat.builder()
							.statut(statut.name())
							.nombre(nombre)
							.pourcentage(pourcentage)
							.build();
				})
				.collect(Collectors.toList());
	}

	private List<VehiculeCarburantStat> getVehiculesParCarburant() {
		return vehiculeRepository.countGroupByEnergie().stream()
				.map(r -> VehiculeCarburantStat.builder()
						.energie((String) r[0])
						.nombre(((Number) r[1]).longValue())
						.build())
				.collect(Collectors.toList());
	}

	private List<RevenuMensuel> getRevenusMensuels(int annee) {
		Map<Integer, Long> facturesMap = factureRepository
				.revenusMensuelsFacturesClient(FactureStatusEnum.PAYEE, annee)
				.stream()
				.filter(r -> r[0] != null && r[1] != null)
				.collect(Collectors.toMap(
					r -> ((Number) r[0]).intValue(),
					r -> ((Number) r[1]).longValue()
				));

		Map<Integer, BigDecimal> missionsMap = missionRepository
				.revenusMensuels(annee)
				.stream()
				.filter(r -> r[0] != null && r[1] != null)
				.collect(Collectors.toMap(
						r -> ((Number) r[0]).intValue(),
						r -> r[1] instanceof BigDecimal ? (BigDecimal) r[1] : BigDecimal.valueOf(((Number) r[1]).doubleValue())
				));

		return IntStream.rangeClosed(1, 12)
				.mapToObj(mois -> {
					long montantFactures = facturesMap.getOrDefault(mois, 0L);
					BigDecimal montantMissions = missionsMap.getOrDefault(mois, BigDecimal.ZERO);
					BigDecimal total = montantMissions.add(BigDecimal.valueOf(montantFactures));
					return RevenuMensuel.builder()
							.mois(mois)
							.montantFactures(montantFactures)
							.montantMissions(montantMissions)
							.total(total)
							.build();
				})
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public VehiculeStats getVehiculeStats(LocalDate dateDebut, LocalDate dateFin) {
		LocalDateTime debut = dateDebut.atStartOfDay();
		LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();

		List<Object[]> resultats = vehiculeRepository.countGroupByStatutBetween(debut, fin);
		long total = vehiculeRepository.countByCreatedAtBetween(debut, fin);

		Map<VehiculeStatusEnum, Long> countMap = resultats.stream()
				.collect(Collectors.toMap(
						r -> (VehiculeStatusEnum) r[0],
						r -> ((Number) r[1]).longValue()
				));

		List<VehiculeStatutStat> parStatut = Arrays.stream(VehiculeStatusEnum.values())
				.map(statut -> {
					long nombre = countMap.getOrDefault(statut, 0L);
					double pourcentage = total > 0 ? Math.round(nombre * 10000.0 / total) / 100.0 : 0.0;
					return VehiculeStatutStat.builder()
							.statut(statut.name())
							.nombre(nombre)
							.pourcentage(pourcentage)
							.build();
				})
				.collect(Collectors.toList());

		return VehiculeStats.builder()
				.total(total)
				.parStatut(parStatut)
				.build();
	}

	@Transactional(readOnly = true)
	public MissionStats getMissionStats(LocalDate dateDebut, LocalDate dateFin) {
		LocalDateTime debut = dateDebut.atStartOfDay();
		LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();

		return MissionStats.builder()
				.total(missionRepository.countByCreatedAtBetween(debut, fin))
				.confirmees(missionRepository.countByIsConfirmerTrueAndCreatedAtBetween(debut, fin))
				.enCours(missionRepository.countEnCours(debut, fin))
				.coutTotalConfirmees(missionRepository.sumMontantConfirmees(debut, fin))
				.build();
	}

	@Transactional(readOnly = true)
	public InterventionStats getInterventionStats(LocalDate dateDebut, LocalDate dateFin) {
		LocalDateTime debut = dateDebut.atStartOfDay();
		LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();

		return InterventionStats.builder()
				.total(interventionRepository.countByCreatedAtBetween(debut, fin))
				.enCours(interventionRepository.countByStatutAndCreatedAtBetween(InterventionStatut.EN_COURS, debut, fin))
				.vehiculesConcernes(interventionRepository.countDistinctVehicules(debut, fin))
				.coutTotal(interventionRepository.sumCout(debut, fin))
				.build();
	}

	@Transactional(readOnly = true)
	public ChauffeurStats getChauffeurStats() {
		LocalDate aujourdhui = LocalDate.now();
		LocalDate finAnnee = LocalDate.of(aujourdhui.getYear(), 12, 31);

		return ChauffeurStats.builder()
				.total(chauffeurRepository.count())
				.permisValides(chauffeurRepository.countByExpDatePermisAfter(aujourdhui))
				.permisExpirentBientot(chauffeurRepository.countByExpDatePermisBetween(aujourdhui, finAnnee))
				.permisExpires(chauffeurRepository.countByExpDatePermisBefore(aujourdhui))
				.build();
	}

	@Transactional(readOnly = true)
	public ProduitStats getProduitStats() {
		return ProduitStats.builder()
				.total(produitRepository.count())
				.enRuptureDeStock(produitRepository.countByStockLessThanEqual(0L))
				.valeurTotaleStock(produitRepository.sumValeurStock())
				.build();
	}

	@Transactional(readOnly = true)
	public LivraisonFournisseurStats getLivraisonFournisseurStats(LocalDate dateDebut, LocalDate dateFin) {
		LocalDateTime debut = dateDebut.atStartOfDay();
		LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();

		return LivraisonFournisseurStats.builder()
				.total(livraisonFournisseurRepository.countByCreatedAtBetween(debut, fin))
				.avecFacture(livraisonFournisseurRepository.countAvecFacture(debut, fin))
				.sansFacture(livraisonFournisseurRepository.countSansFacture(debut, fin))
				.produitsEntres(entreeProduitRepository.sumQuantiteBetween(debut, fin))
				.build();
	}

	@Transactional(readOnly = true)
	public LivraisonClientStats getLivraisonClientStats(LocalDate dateDebut, LocalDate dateFin) {
		LocalDateTime debut = dateDebut.atStartOfDay();
		LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();

		return LivraisonClientStats.builder()
				.total(livraisonClientRepository.countByCreatedAtBetween(debut, fin))
				.produitsSortis(sortieProduitRepository.sumQuantiteBetween(debut, fin))
				.build();
	}

	@Transactional(readOnly = true)
	public CompteStats getCompteStats() {
		return CompteStats.builder()
				.total(compteRepository.count())
				.balanceTotale(compteRepository.sumBalance())
				.totalSoldesPositifs(compteRepository.sumSoldesPositifs())
				.totalSoldesNegatifs(compteRepository.sumSoldesNegatifs())
				.build();
	}

	@Transactional(readOnly = true)
	public FactureStats getFactureStats(boolean factureClient, LocalDate dateDebut, LocalDate dateFin) {
		LocalDateTime debut = dateDebut.atStartOfDay();
		LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();

		long enAttente = factureRepository.countByFactureClientAndStatutAndCreatedAtBetween(factureClient, FactureStatusEnum.PROFORMA, debut, fin)
				+ factureRepository.countByFactureClientAndStatutAndCreatedAtBetween(factureClient, FactureStatusEnum.FACTUREE, debut, fin);

		return FactureStats.builder()
				.total(factureRepository.countByFactureClientAndCreatedAtBetween(factureClient, debut, fin))
				.enAttente(enAttente)
				.payees(factureRepository.countByFactureClientAndStatutAndCreatedAtBetween(factureClient, FactureStatusEnum.PAYEE, debut, fin))
				.montantTotalPaye(factureRepository.sumMontantPayeByFactureClient(factureClient, FactureStatusEnum.PAYEE, debut, fin))
				.montantTotal(factureRepository.sumMontantTotalByFactureClient(factureClient, debut, fin))
				.build();
	}
}