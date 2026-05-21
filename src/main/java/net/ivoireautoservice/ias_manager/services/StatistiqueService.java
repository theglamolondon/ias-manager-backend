package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
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
	public VehiculeStats getVehiculeStats() {
		List<Object[]> resultats = vehiculeRepository.countGroupByStatut();
		long total = vehiculeRepository.count();

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
				.enCours(missionRepository.countEnCours(debut, fin))
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
				.total(livraisonFournisseurRepository.countValidesBetween(debut, fin))
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
		Object[] impayees = factureRepository.countAndSumFacturesImpayees()
				.stream().findFirst().orElse(new Object[]{0L, 0L});
		return CompteStats.builder()
				.total(compteRepository.count())
				.balanceTotale(compteRepository.sumBalance())
				.totalSoldesPositifs(compteRepository.sumSoldesPositifs())
				.totalSoldesNegatifs(compteRepository.sumSoldesNegatifs())
				.facturesImpayeesNombre(((Number) impayees[0]).longValue())
				.facturesImpayeesMontant(((Number) impayees[1]).longValue())
				.build();
	}

	@Transactional(readOnly = true)
	public RapportFinancier getRapportFinancier(LocalDateTime debut, LocalDateTime fin) {
		LocalDate aujourdhui = LocalDate.now();
		LocalDate j30 = aujourdhui.minusDays(30);
		LocalDate j60 = aujourdhui.minusDays(60);
		LocalDate j90 = aujourdhui.minusDays(90);

		// 1. KPIs principaux (1 requête)
		List<Object[]> kpisList = factureRepository.rapportFinancierKpis(aujourdhui, debut, fin);
		Object[] kpis = kpisList.isEmpty() ? new Object[]{0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L} : kpisList.get(0);
		long chiffreAffaire = toLong(kpis[0]);
		long montantEncaisse = toLong(kpis[1]);
		long nombreEncaissees = toLong(kpis[2]);
		long montantAVenir = toLong(kpis[3]);
		long nombreAVenir = toLong(kpis[4]);
		long montantEnRetard = toLong(kpis[5]);
		long nombreEnRetard = toLong(kpis[6]);
		long nombreTotal = toLong(kpis[7]);
		double pourcentageRecouvrement = chiffreAffaire > 0
				? Math.round(montantEncaisse * 10000.0 / chiffreAffaire) / 100.0 : 0.0;
		double dsoGlobal = chiffreAffaire > 0
				? Math.round((montantEnRetard + montantAVenir) * 365.0 / chiffreAffaire * 100.0) / 100.0 : 0.0;

		// 2. Balance âgée (1 requête)
		List<Object[]> baList = factureRepository.rapportBalanceAgee(aujourdhui, j30, j60, j90, debut, fin);
		Object[] ba = baList.isEmpty() ? new Object[]{0L, 0L, 0L, 0L, 0L} : baList.get(0);
		RapportFinancier.BalanceAgee balanceAgee = RapportFinancier.BalanceAgee.builder()
				.nonEchu(toLong(ba[0]))
				.echu0a30(toLong(ba[1]))
				.echu31a60(toLong(ba[2]))
				.echu61a90(toLong(ba[3]))
				.echuPlus90(toLong(ba[4]))
				.build();

		// 3. Rentrées de fonds mensuelles (1 requête)
		List<Object[]> rentrees = factureRepository.rapportRentreesFondsMensuelles(debut, fin);
		List<RapportFinancier.RentreeFonds> rentreesFonds = rentrees.stream()
				.map(r -> {
					long ca = toLong(r[2]);
					long enc = toLong(r[3]);
					return RapportFinancier.RentreeFonds.builder()
							.annee(((Number) r[0]).intValue())
							.mois(((Number) r[1]).intValue())
							.chiffreAffaire(ca)
							.encaissement(enc)
							.prevision(enc) // prévision = encaissement réel pour les mois passés
							.build();
				})
				.collect(Collectors.toList());

		// Calculer prévision pour les 3 prochains mois (moyenne glissante)
		long moyenneEncaissement = rentreesFonds.isEmpty() ? 0 :
				rentreesFonds.stream().mapToLong(RapportFinancier.RentreeFonds::getEncaissement).sum() / rentreesFonds.size();
		YearMonth moisCourant = YearMonth.now();
		for (int i = 1; i <= 3; i++) {
			YearMonth moisFutur = moisCourant.plusMonths(i);
			boolean existe = rentreesFonds.stream()
					.anyMatch(r -> r.getAnnee() == moisFutur.getYear() && r.getMois() == moisFutur.getMonthValue());
			if (!existe) {
				rentreesFonds.add(RapportFinancier.RentreeFonds.builder()
						.annee(moisFutur.getYear())
						.mois(moisFutur.getMonthValue())
						.chiffreAffaire(0)
						.encaissement(0)
						.prevision(moyenneEncaissement)
						.build());
			}
		}

		// 4. Évolution DSO mensuel (1 requête, déjà récupérée)
		List<Object[]> dsoData = factureRepository.rapportDsoMensuel(debut, fin);
		List<RapportFinancier.DsoMensuel> evolutionDso = dsoData.stream()
				.map(r -> {
					long creances = toLong(r[2]);
					long ca = toLong(r[3]);
					int annee = ((Number) r[0]).intValue();
					int mois = ((Number) r[1]).intValue();
					int joursInMonth = YearMonth.of(annee, mois).lengthOfMonth();
					double dso = ca > 0 ? Math.round(creances * (double) joursInMonth / ca * 100.0) / 100.0 : 0.0;
					return RapportFinancier.DsoMensuel.builder()
							.annee(annee)
							.mois(mois)
							.dso(dso)
							.build();
				})
				.collect(Collectors.toList());

		// 5. Top 5 factures échues (1 requête)
		List<Object[]> echues = factureRepository.rapportFacturesEchuesTop5(
				aujourdhui, debut, fin, PageRequest.of(0, 5));
		List<RapportFinancier.FactureEchue> facturesEchues = echues.stream()
				.map(r -> RapportFinancier.FactureEchue.builder()
						.factureId(toLong(r[0]))
						.numFacture((String) r[1])
						.nomClient((String) r[2])
						.joursRetard(ChronoUnit.DAYS.between((LocalDate) r[3], aujourdhui))
						.solde(toLong(r[4]))
						.build())
				.collect(Collectors.toList());

		// 6. Top 5 clients impayés (1 requête)
		List<Object[]> clients = factureRepository.rapportTopClientsImpayes(
				debut, fin, PageRequest.of(0, 5));
		List<RapportFinancier.ClientImpaye> topClientsImpayes = clients.stream()
				.map(r -> RapportFinancier.ClientImpaye.builder()
						.partenaireId(toLong(r[0]))
						.nomClient((String) r[1])
						.montantImpaye(toLong(r[2]))
						.build())
				.collect(Collectors.toList());

		// 7. Montant à relancer (1 requête)
		Object[] rel = factureRepository.rapportMontantRelance(aujourdhui, j30, j60, debut, fin)
				.stream().findFirst().orElse(new Object[]{0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L});
		RapportFinancier.MontantRelance montantRelance = RapportFinancier.MontantRelance.builder()
				.totalMontant(toLong(rel[0]))
				.totalNombre(toLong(rel[1]))
				.relance1Montant(toLong(rel[2]))
				.relance1Nombre(toLong(rel[3]))
				.relance2Montant(toLong(rel[4]))
				.relance2Nombre(toLong(rel[5]))
				.relance3Montant(toLong(rel[6]))
				.relance3Nombre(toLong(rel[7]))
				.build();

		return RapportFinancier.builder()
				.chiffreAffaire(chiffreAffaire)
				.montantEncaisse(montantEncaisse)
				.nombreFacturesEncaissees(nombreEncaissees)
				.montantAVenir(montantAVenir)
				.nombreFacturesAVenir(nombreAVenir)
				.montantEnRetard(montantEnRetard)
				.nombreFacturesEnRetard(nombreEnRetard)
				.pourcentageRecouvrement(pourcentageRecouvrement)
				.nombreTotalFactures(nombreTotal)
				.dsoJours(dsoGlobal)
				.balanceAgee(balanceAgee)
				.rentreesFonds(rentreesFonds)
				.evolutionDso(evolutionDso)
				.facturesEchues(facturesEchues)
				.topClientsImpayes(topClientsImpayes)
				.montantRelance(montantRelance)
				.build();
	}

	@Transactional(readOnly = true)
	public FactureStats getFactureStats(boolean factureClient, LocalDate dateDebut, LocalDate dateFin) {
		LocalDateTime debut = dateDebut.atStartOfDay();
		LocalDateTime fin = dateFin.plusDays(1).atStartOfDay();

		// Une seule requête groupée par statut (count + sum)
		List<StatutAgregat> agregats = factureRepository.statsParStatut(factureClient, debut, fin);
		Map<FactureStatusEnum, StatutAgregat> map = agregats.stream()
				.collect(Collectors.toMap(StatutAgregat::getStatut, a -> a));

		StatutAgregat proforma = map.getOrDefault(FactureStatusEnum.PROFORMA, new StatutAgregat(FactureStatusEnum.PROFORMA, 0, 0));
		StatutAgregat facturee = map.getOrDefault(FactureStatusEnum.FACTUREE, new StatutAgregat(FactureStatusEnum.FACTUREE, 0, 0));
		StatutAgregat payee = map.getOrDefault(FactureStatusEnum.PAYEE, new StatutAgregat(FactureStatusEnum.PAYEE, 0, 0));
		StatutAgregat annulee = map.getOrDefault(FactureStatusEnum.ANNULEE, new StatutAgregat(FactureStatusEnum.ANNULEE, 0, 0));

		long enAttenteNombre = proforma.getNombre() + facturee.getNombre();
		long enAttenteMontant = proforma.getMontant() + facturee.getMontant();
		long totalNombre = agregats.stream().mapToLong(StatutAgregat::getNombre).sum();
		long totalMontant = agregats.stream()
				.filter(a -> a.getStatut() != FactureStatusEnum.ANNULEE)
				.mapToLong(StatutAgregat::getMontant).sum();

		return FactureStats.builder()
				.total(totalNombre)
				.enAttente(enAttenteNombre)
				.annulees(annulee.getNombre())
				.payees(payee.getNombre())
				.impayees(enAttenteNombre)
				.montantTotal(totalMontant)
				.montantTotalEnAttente(enAttenteMontant)
				.montantTotalPaye(payee.getMontant())
				.montantTotalImpaye(enAttenteMontant)
				.build();
	}

	@Transactional(readOnly = true)
	public RecapitulatifMensuel getRecapitulatifMensuel(int annee) {
		Map<Integer, Long> revenusFacturesMap = factureRepository
				.revenusMensuelsFacturesClient(FactureStatusEnum.PAYEE, annee).stream()
				.filter(r -> r[0] != null && r[1] != null)
				.collect(Collectors.toMap(r -> ((Number) r[0]).intValue(), r -> ((Number) r[1]).longValue()));

		Map<Integer, Long> revenusMissionsMap = missionRepository
				.revenusMensuels(annee).stream()
				.filter(r -> r[0] != null && r[1] != null)
				.collect(Collectors.toMap(
						r -> ((Number) r[0]).intValue(),
						r -> r[1] instanceof BigDecimal
								? ((BigDecimal) r[1]).longValue()
								: ((Number) r[1]).longValue()
				));

		Map<Integer, Long> depensesInterventionsMap = interventionRepository
				.coutMensuel(annee).stream()
				.filter(r -> r[0] != null && r[1] != null)
				.collect(Collectors.toMap(r -> ((Number) r[0]).intValue(), r -> ((Number) r[1]).longValue()));

		Map<Integer, Long> depensesFacturesMap = factureRepository
				.depensesMensuellesFournisseur(FactureStatusEnum.PAYEE, annee).stream()
				.filter(r -> r[0] != null && r[1] != null)
				.collect(Collectors.toMap(r -> ((Number) r[0]).intValue(), r -> ((Number) r[1]).longValue()));

		long totalVehicules = vehiculeRepository.count();
		Map<Integer, Long> vehiculesUtilisesMap = missionRepository
				.vehiculesUtilisesMensuel(annee).stream()
				.filter(r -> r[0] != null && r[1] != null)
				.collect(Collectors.toMap(r -> ((Number) r[0]).intValue(), r -> ((Number) r[1]).longValue()));

		List<RecapMensuelItem> lignes = IntStream.rangeClosed(1, 12)
				.mapToObj(mois -> {
					long revenus = revenusFacturesMap.getOrDefault(mois, 0L)
							+ revenusMissionsMap.getOrDefault(mois, 0L);
					long depenses = depensesInterventionsMap.getOrDefault(mois, 0L)
							+ depensesFacturesMap.getOrDefault(mois, 0L);
					long vehiculesUtilises = vehiculesUtilisesMap.getOrDefault(mois, 0L);
					double tauxUtilisation = totalVehicules > 0
							? Math.round(vehiculesUtilises * 10000.0 / totalVehicules) / 100.0 : 0.0;
					return RecapMensuelItem.builder()
							.annee(annee)
							.mois(mois)
							.revenus(revenus)
							.depenses(depenses)
							.marge(revenus - depenses)
							.tauxUtilisation(tauxUtilisation)
							.build();
				})
				.collect(Collectors.toList());

		long totalRevenus = lignes.stream().mapToLong(RecapMensuelItem::getRevenus).sum();
		long totalDepenses = lignes.stream().mapToLong(RecapMensuelItem::getDepenses).sum();
		double tauxMoyen = lignes.stream().mapToDouble(RecapMensuelItem::getTauxUtilisation).average().orElse(0.0);

		return RecapitulatifMensuel.builder()
				.lignes(lignes)
				.totalRevenus(totalRevenus)
				.totalDepenses(totalDepenses)
				.totalMarge(totalRevenus - totalDepenses)
				.tauxUtilisationMoyen(Math.round(tauxMoyen * 100.0) / 100.0)
				.build();
	}

	private static long toLong(Object value) {
		return value instanceof Number ? ((Number) value).longValue() : 0L;
	}
}