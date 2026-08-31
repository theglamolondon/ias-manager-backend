package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StatistiqueService — agrégats du tableau de bord et rapports")
class StatistiqueServiceTest {

	@Mock private VehiculeRepository vehiculeRepository;
	@Mock private FactureRepository factureRepository;
	@Mock private MissionRepository missionRepository;
	@Mock private InterventionRepository interventionRepository;
	@Mock private ChauffeurRepository chauffeurRepository;
	@Mock private ProduitRepository produitRepository;
	@Mock private LivraisonFournisseurRepository livraisonFournisseurRepository;
	@Mock private LivraisonClientRepository livraisonClientRepository;
	@Mock private CompteRepository compteRepository;
	@Mock private LigneCompteRepository ligneCompteRepository;
	@Mock private EntreeProduitRepository entreeProduitRepository;
	@Mock private SortieProduitRepository sortieProduitRepository;
	@Mock private PartenaireRepository partenaireRepository;
	@Mock private SecurityService securityService;

	@InjectMocks
	private StatistiqueService service;

	@Nested
	@DisplayName("Répartition de la flotte")
	class Flotte {

		@Test
		@DisplayName("chaque statut du référentiel est présent, y compris ceux à zéro")
		void tousLesStatutsPresents() {
			when(vehiculeRepository.countGroupByStatut()).thenReturn(List.<Object[]>of(
					new Object[]{VehiculeStatusEnum.DISPONIBLE, 6L},
					new Object[]{VehiculeStatusEnum.MISSION, 2L}));
			when(vehiculeRepository.countGroupByEnergie()).thenReturn(List.of());
			when(factureRepository.revenusMensuelsFacturesClient(any(), anyInt())).thenReturn(List.of());
			when(missionRepository.revenusMensuels(anyInt())).thenReturn(List.of());

			StatistiqueDashboard dashboard = service.getDashboard(2026);

			assertThat(dashboard.getVehiculesParStatut())
					.hasSize(VehiculeStatusEnum.values().length)
					.extracting(VehiculeStatutStat::getStatut)
					.contains("DISPONIBLE", "MISSION", "GARAGE", "SINISTRE");
		}

		@Test
		@DisplayName("les pourcentages sont calculés sur le total et arrondis à deux décimales")
		void pourcentages() {
			when(vehiculeRepository.countGroupByStatut()).thenReturn(List.<Object[]>of(
					new Object[]{VehiculeStatusEnum.DISPONIBLE, 1L},
					new Object[]{VehiculeStatusEnum.MISSION, 2L}));
			when(vehiculeRepository.countGroupByEnergie()).thenReturn(List.of());
			when(factureRepository.revenusMensuelsFacturesClient(any(), anyInt())).thenReturn(List.of());
			when(missionRepository.revenusMensuels(anyInt())).thenReturn(List.of());

			var stats = service.getDashboard(2026).getVehiculesParStatut();

			assertThat(stats).filteredOn(s -> "DISPONIBLE".equals(s.getStatut()))
					.first().extracting(VehiculeStatutStat::getPourcentage).isEqualTo(33.33);
			assertThat(stats).filteredOn(s -> "MISSION".equals(s.getStatut()))
					.first().extracting(VehiculeStatutStat::getPourcentage).isEqualTo(66.67);
		}

		@Test
		@DisplayName("une flotte vide ne provoque pas de division par zéro")
		void flotteVide() {
			when(vehiculeRepository.countGroupByStatut()).thenReturn(List.of());
			when(vehiculeRepository.countGroupByEnergie()).thenReturn(List.of());
			when(factureRepository.revenusMensuelsFacturesClient(any(), anyInt())).thenReturn(List.of());
			when(missionRepository.revenusMensuels(anyInt())).thenReturn(List.of());

			assertThat(service.getDashboard(2026).getVehiculesParStatut())
					.allMatch(s -> s.getNombre() == 0 && s.getPourcentage() == 0.0);
		}

		@Test
		@DisplayName("getVehiculeStats calcule les pourcentages sur le total réel de la table")
		void vehiculeStats() {
			when(vehiculeRepository.countGroupByStatut()).thenReturn(List.<Object[]>of(
					new Object[]{VehiculeStatusEnum.DISPONIBLE, 5L}));
			when(vehiculeRepository.count()).thenReturn(10L);

			VehiculeStats stats = service.getVehiculeStats();

			assertThat(stats.getTotal()).isEqualTo(10L);
			assertThat(stats.getParStatut()).filteredOn(s -> "DISPONIBLE".equals(s.getStatut()))
					.first().extracting(VehiculeStatutStat::getPourcentage).isEqualTo(50.0);
		}

		@Test
		@DisplayName("la répartition par carburant reprend les libellés issus de la base")
		void parCarburant() {
			when(vehiculeRepository.countGroupByStatut()).thenReturn(List.of());
			when(vehiculeRepository.countGroupByEnergie()).thenReturn(List.<Object[]>of(
					new Object[]{"Diesel", 7L}, new Object[]{"Essence", 3L}));
			when(factureRepository.revenusMensuelsFacturesClient(any(), anyInt())).thenReturn(List.of());
			when(missionRepository.revenusMensuels(anyInt())).thenReturn(List.of());

			assertThat(service.getDashboard(2026).getVehiculesParCarburant())
					.extracting(VehiculeCarburantStat::getEnergie).containsExactly("Diesel", "Essence");
		}
	}

	@Nested
	@DisplayName("Revenus mensuels")
	class Revenus {

		@Test
		@DisplayName("les douze mois sont toujours présents, les mois sans donnée à zéro")
		void douzeMois() {
			when(vehiculeRepository.countGroupByStatut()).thenReturn(List.of());
			when(vehiculeRepository.countGroupByEnergie()).thenReturn(List.of());
			when(factureRepository.revenusMensuelsFacturesClient(FactureStatusEnum.PAYEE, 2026))
					.thenReturn(List.<Object[]>of(new Object[]{3, 500_000L}));
			when(missionRepository.revenusMensuels(2026)).thenReturn(List.of());

			List<RevenuMensuel> revenus = service.getDashboard(2026).getRevenusMensuels();

			assertThat(revenus).hasSize(12);
			assertThat(revenus.get(2).getMois()).isEqualTo(3);
			assertThat(revenus.get(2).getMontantFactures()).isEqualTo(500_000L);
			assertThat(revenus.get(0).getMontantFactures()).isZero();
		}

		@Test
		@DisplayName("le total mensuel additionne factures payées et revenus de missions")
		void totalMensuel() {
			when(vehiculeRepository.countGroupByStatut()).thenReturn(List.of());
			when(vehiculeRepository.countGroupByEnergie()).thenReturn(List.of());
			when(factureRepository.revenusMensuelsFacturesClient(FactureStatusEnum.PAYEE, 2026))
					.thenReturn(List.<Object[]>of(new Object[]{1, 500_000L}));
			when(missionRepository.revenusMensuels(2026))
					.thenReturn(List.<Object[]>of(new Object[]{1, BigDecimal.valueOf(300_000)}));

			RevenuMensuel janvier = service.getDashboard(2026).getRevenusMensuels().get(0);

			assertThat(janvier.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(800_000));
		}

		@Test
		@DisplayName("les lignes incomplètes remontées par la base sont ignorées")
		void lignesIncompletes() {
			when(vehiculeRepository.countGroupByStatut()).thenReturn(List.of());
			when(vehiculeRepository.countGroupByEnergie()).thenReturn(List.of());
			when(factureRepository.revenusMensuelsFacturesClient(any(), anyInt()))
					.thenReturn(List.<Object[]>of(new Object[]{null, 500_000L}, new Object[]{2, null}));
			when(missionRepository.revenusMensuels(anyInt())).thenReturn(List.of());

			assertThat(service.getDashboard(2026).getRevenusMensuels())
					.allMatch(r -> r.getMontantFactures() == 0L);
		}
	}

	@Nested
	@DisplayName("Statistiques de factures")
	class Factures {

		private List<StatutAgregat> agregats() {
			return List.of(
					new StatutAgregat(FactureStatusEnum.PROFORMA, 2, 100_000),
					new StatutAgregat(FactureStatusEnum.FACTUREE, 3, 200_000),
					new StatutAgregat(FactureStatusEnum.PAYEE, 5, 700_000),
					new StatutAgregat(FactureStatusEnum.ANNULEE, 1, 50_000));
		}

		@Test
		@DisplayName("en attente = proforma + facturée, en nombre comme en montant")
		void enAttente() {
			when(factureRepository.statsParStatut(anyBoolean(), any(), any())).thenReturn(agregats());

			FactureStats stats = service.getFactureStats(true,
					LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

			assertThat(stats.getEnAttente()).isEqualTo(5L);
			assertThat(stats.getMontantTotalEnAttente()).isEqualTo(300_000L);
			assertThat(stats.getImpayees()).isEqualTo(5L);
			assertThat(stats.getMontantTotalImpaye()).isEqualTo(300_000L);
		}

		@Test
		@DisplayName("le montant total exclut les factures annulées, mais le nombre total les compte")
		void totaux() {
			when(factureRepository.statsParStatut(anyBoolean(), any(), any())).thenReturn(agregats());

			FactureStats stats = service.getFactureStats(true,
					LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

			assertThat(stats.getTotal()).isEqualTo(11L);
			assertThat(stats.getMontantTotal()).isEqualTo(1_000_000L);
			assertThat(stats.getAnnulees()).isEqualTo(1L);
			assertThat(stats.getPayees()).isEqualTo(5L);
			assertThat(stats.getMontantTotalPaye()).isEqualTo(700_000L);
		}

		@Test
		@DisplayName("un statut absent est traité comme zéro")
		void statutAbsent() {
			when(factureRepository.statsParStatut(anyBoolean(), any(), any()))
					.thenReturn(List.of(new StatutAgregat(FactureStatusEnum.PAYEE, 1, 10_000)));

			FactureStats stats = service.getFactureStats(false,
					LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

			assertThat(stats.getEnAttente()).isZero();
			assertThat(stats.getAnnulees()).isZero();
			assertThat(stats.getMontantTotal()).isEqualTo(10_000L);
		}

		@Test
		@DisplayName("la borne de fin est exclusive au lendemain de la date demandée")
		void bornesDeDates() {
			when(factureRepository.statsParStatut(anyBoolean(), any(), any())).thenReturn(List.of());

			service.getFactureStats(true, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

			verify(factureRepository).statsParStatut(true,
					LocalDateTime.of(2026, 3, 1, 0, 0),
					LocalDateTime.of(2026, 4, 1, 0, 0));
		}
	}

	@Nested
	@DisplayName("Récapitulatif mensuel")
	class Recapitulatif {

		private void stubVide() {
			when(factureRepository.revenusMensuelsFacturesClient(any(), anyInt())).thenReturn(List.of());
			when(missionRepository.revenusMensuels(anyInt())).thenReturn(List.of());
			when(interventionRepository.coutMensuel(anyInt())).thenReturn(List.of());
			when(factureRepository.depensesMensuellesFournisseur(any(), anyInt())).thenReturn(List.of());
			when(missionRepository.vehiculesUtilisesMensuel(anyInt())).thenReturn(List.of());
		}

		@Test
		@DisplayName("la marge mensuelle est la différence revenus − dépenses")
		void marge() {
			stubVide();
			when(factureRepository.revenusMensuelsFacturesClient(FactureStatusEnum.PAYEE, 2026))
					.thenReturn(List.<Object[]>of(new Object[]{1, 1_000_000L}));
			when(interventionRepository.coutMensuel(2026))
					.thenReturn(List.<Object[]>of(new Object[]{1, 300_000L}));
			when(vehiculeRepository.count()).thenReturn(10L);

			RecapitulatifMensuel recap = service.getRecapitulatifMensuel(2026);

			RecapMensuelItem janvier = recap.getLignes().get(0);
			assertThat(janvier.getRevenus()).isEqualTo(1_000_000L);
			assertThat(janvier.getDepenses()).isEqualTo(300_000L);
			assertThat(janvier.getMarge()).isEqualTo(700_000L);
		}

		@Test
		@DisplayName("les totaux annuels agrègent les douze mois")
		void totauxAnnuels() {
			stubVide();
			when(factureRepository.revenusMensuelsFacturesClient(FactureStatusEnum.PAYEE, 2026))
					.thenReturn(List.<Object[]>of(new Object[]{1, 500_000L}, new Object[]{2, 500_000L}));
			when(factureRepository.depensesMensuellesFournisseur(FactureStatusEnum.PAYEE, 2026))
					.thenReturn(List.<Object[]>of(new Object[]{1, 200_000L}));

			RecapitulatifMensuel recap = service.getRecapitulatifMensuel(2026);

			assertThat(recap.getLignes()).hasSize(12);
			assertThat(recap.getTotalRevenus()).isEqualTo(1_000_000L);
			assertThat(recap.getTotalDepenses()).isEqualTo(200_000L);
			assertThat(recap.getTotalMarge()).isEqualTo(800_000L);
		}

		@Test
		@DisplayName("le taux d'utilisation rapporte les véhicules utilisés à la flotte totale")
		void tauxUtilisation() {
			stubVide();
			when(missionRepository.vehiculesUtilisesMensuel(2026))
					.thenReturn(List.<Object[]>of(new Object[]{1, 3L}));
			when(vehiculeRepository.count()).thenReturn(12L);

			RecapitulatifMensuel recap = service.getRecapitulatifMensuel(2026);

			assertThat(recap.getLignes().get(0).getTauxUtilisation()).isEqualTo(25.0);
			assertThat(recap.getTauxUtilisationMoyen()).isEqualTo(2.08);
		}

		@Test
		@DisplayName("une flotte vide donne un taux d'utilisation nul, sans erreur")
		void flotteVide() {
			stubVide();
			when(vehiculeRepository.count()).thenReturn(0L);

			assertThat(service.getRecapitulatifMensuel(2026).getLignes())
					.allMatch(l -> l.getTauxUtilisation() == 0.0);
		}

		@Test
		@DisplayName("les revenus de missions en BigDecimal sont correctement convertis")
		void revenusMissionsBigDecimal() {
			stubVide();
			when(missionRepository.revenusMensuels(2026))
					.thenReturn(List.<Object[]>of(new Object[]{5, BigDecimal.valueOf(750_000)}));

			assertThat(service.getRecapitulatifMensuel(2026).getLignes().get(4).getRevenus())
					.isEqualTo(750_000L);
		}
	}

	@Nested
	@DisplayName("Alertes documentaires")
	class Alertes {

		private VehiculeEntity vehicule(LocalDate finAssurance) {
			return VehiculeEntity.builder()
					.id(1L).immatriculation("AB-123-CD").finValiditeAssurance(finAssurance).build();
		}

		private void stubAucuneAlerte() {
			when(vehiculeRepository.findAssurancesExpirees(any())).thenReturn(List.of());
			when(vehiculeRepository.findAssurancesExpirentBientot(any(), any())).thenReturn(List.of());
			when(vehiculeRepository.findVisitesExpirees(any())).thenReturn(List.of());
			when(vehiculeRepository.findVisitesExpirentBientot(any(), any())).thenReturn(List.of());
			when(vehiculeRepository.findPatentesExpirees(any())).thenReturn(List.of());
			when(vehiculeRepository.findPatentesExpirentBientot(any(), any())).thenReturn(List.of());
			when(vehiculeRepository.findCartesStationnementExpirees(any())).thenReturn(List.of());
			when(vehiculeRepository.findCartesStationnementExpirentBientot(any(), any())).thenReturn(List.of());
			when(vehiculeRepository.findCartesTransportExpirees(any())).thenReturn(List.of());
			when(vehiculeRepository.findCartesTransportExpirentBientot(any(), any())).thenReturn(List.of());
		}

		@Test
		@DisplayName("les compteurs reflètent la taille des listes remontées")
		void compteurs() {
			stubAucuneAlerte();
			when(vehiculeRepository.findAssurancesExpirees(any()))
					.thenReturn(List.of(vehicule(LocalDate.now().minusDays(5))));

			VehiculeAlertesStats stats = service.getVehiculeAlertesStats();

			assertThat(stats.getAssurancesExpirees()).isEqualTo(1);
			assertThat(stats.getVehiculesAssuranceExpiree()).hasSize(1);
			assertThat(stats.getVisitesExpirees()).isZero();
		}

		@Test
		@DisplayName("les jours restants sont négatifs pour un document expiré")
		void joursRestantsNegatifs() {
			stubAucuneAlerte();
			when(vehiculeRepository.findAssurancesExpirees(any()))
					.thenReturn(List.of(vehicule(LocalDate.now().minusDays(5))));

			AlerteVehicule alerte = service.getVehiculeAlertesStats().getVehiculesAssuranceExpiree().get(0);

			assertThat(alerte.getJoursRestantsAssurance()).isEqualTo(-5L);
			assertThat(alerte.getImmatriculation()).isEqualTo("AB-123-CD");
		}

		@Test
		@DisplayName("un document sans date donne zéro jour restant, sans erreur")
		void sansDate() {
			stubAucuneAlerte();
			when(vehiculeRepository.findAssurancesExpirentBientot(any(), any()))
					.thenReturn(List.of(vehicule(null)));

			AlerteVehicule alerte = service.getVehiculeAlertesStats()
					.getVehiculesAssuranceExpireBientot().get(0);

			assertThat(alerte.getJoursRestantsAssurance()).isZero();
			assertThat(alerte.getJoursRestantsVisite()).isZero();
		}
	}

	@Nested
	@DisplayName("Autres agrégats")
	class AutresAgregats {

		@Test
		@DisplayName("le trésorier en chef obtient les agrégats de tous les comptes et les impayés")
		void compteStats() {
			when(securityService.hasAuthority(PermissionEnum.TRESORERIE_ADMIN)).thenReturn(true);
			when(compteRepository.statistiques(null))
					.thenReturn(List.<Object[]>of(new Object[]{3L, 1_500_000L, 2_000_000L, -500_000L}));
			when(factureRepository.countAndSumFacturesImpayees())
					.thenReturn(List.<Object[]>of(new Object[]{4L, 900_000L}));

			CompteStats stats = service.getCompteStats();

			assertThat(stats.getTotal()).isEqualTo(3L);
			assertThat(stats.getBalanceTotale()).isEqualTo(1_500_000L);
			assertThat(stats.getTotalSoldesPositifs()).isEqualTo(2_000_000L);
			assertThat(stats.getTotalSoldesNegatifs()).isEqualTo(-500_000L);
			assertThat(stats.getFacturesImpayeesNombre()).isEqualTo(4L);
			assertThat(stats.getFacturesImpayeesMontant()).isEqualTo(900_000L);
		}

		@Test
		@DisplayName("l'absence d'impayés retombe sur des compteurs à zéro")
		void compteStatsSansImpaye() {
			when(securityService.hasAuthority(PermissionEnum.TRESORERIE_ADMIN)).thenReturn(true);
			when(compteRepository.statistiques(null))
					.thenReturn(List.<Object[]>of(new Object[]{0L, 0L, 0L, 0L}));
			when(factureRepository.countAndSumFacturesImpayees()).thenReturn(List.of());

			CompteStats stats = service.getCompteStats();

			assertThat(stats.getFacturesImpayeesNombre()).isZero();
			assertThat(stats.getFacturesImpayeesMontant()).isZero();
		}

		@Test
		@DisplayName("sans TRESORERIE_ADMIN, les agrégats sont limités aux comptes de l'utilisateur et les impayés masqués")
		void compteStatsPerimetreUtilisateur() {
			when(securityService.hasAuthority(PermissionEnum.TRESORERIE_ADMIN)).thenReturn(false);
			when(securityService.getUtilisateurConnecte())
					.thenReturn(Utilisateur.builder().id(7L).build());
			when(compteRepository.statistiques(7L))
					.thenReturn(List.<Object[]>of(new Object[]{1L, 250_000L, 250_000L, 0L}));

			CompteStats stats = service.getCompteStats();

			assertThat(stats.getTotal()).isEqualTo(1L);
			assertThat(stats.getBalanceTotale()).isEqualTo(250_000L);
			assertThat(stats.getFacturesImpayeesNombre()).isZero();
			assertThat(stats.getFacturesImpayeesMontant()).isZero();
			verify(factureRepository, never()).countAndSumFacturesImpayees();
		}

		@Test
		@DisplayName("les statistiques chauffeurs distinguent permis valides, bientôt expirés et expirés")
		void chauffeurStats() {
			when(chauffeurRepository.count()).thenReturn(10L);
			when(chauffeurRepository.countByExpDatePermisAfter(any())).thenReturn(7L);
			when(chauffeurRepository.countByExpDatePermisBetween(any(), any())).thenReturn(2L);
			when(chauffeurRepository.countByExpDatePermisBefore(any())).thenReturn(3L);

			ChauffeurStats stats = service.getChauffeurStats();

			assertThat(stats.getTotal()).isEqualTo(10L);
			assertThat(stats.getPermisValides()).isEqualTo(7L);
			assertThat(stats.getPermisExpirentBientot()).isEqualTo(2L);
			assertThat(stats.getPermisExpires()).isEqualTo(3L);
		}

		@Test
		@DisplayName("les statistiques produits exposent ruptures et valeur du stock")
		void produitStats() {
			when(produitRepository.count()).thenReturn(120L);
			when(produitRepository.countByStockLessThanEqual(0L)).thenReturn(4L);
			when(produitRepository.sumValeurStock()).thenReturn(8_000_000L);

			ProduitStats stats = service.getProduitStats();

			assertThat(stats.getTotal()).isEqualTo(120L);
			assertThat(stats.getEnRuptureDeStock()).isEqualTo(4L);
			assertThat(stats.getValeurTotaleStock()).isEqualTo(8_000_000L);
		}

		@Test
		@DisplayName("les statistiques partenaires distinguent clients et fournisseurs")
		void partenaireStats() {
			when(partenaireRepository.count()).thenReturn(30L);
			when(partenaireRepository.countByIsClientTrue()).thenReturn(20L);
			when(partenaireRepository.countByIsFournisseurTrue()).thenReturn(15L);

			PartenaireStats stats = service.getPartenaireStats();

			assertThat(stats.getTotal()).isEqualTo(30L);
			assertThat(stats.getClients()).isEqualTo(20L);
			assertThat(stats.getFournisseurs()).isEqualTo(15L);
		}

		@Test
		@DisplayName("les statistiques d'interventions bornent la période au lendemain de la date de fin")
		void interventionStats() {
			LocalDateTime debut = LocalDateTime.of(2026, 3, 1, 0, 0);
			LocalDateTime fin = LocalDateTime.of(2026, 4, 1, 0, 0);
			when(interventionRepository.countByCreatedAtBetween(debut, fin)).thenReturn(8L);
			when(interventionRepository.countByStatutAndCreatedAtBetween(
					InterventionStatut.EN_COURS, debut, fin)).thenReturn(3L);
			when(interventionRepository.countDistinctVehicules(debut, fin)).thenReturn(5L);
			when(interventionRepository.sumCout(debut, fin)).thenReturn(450_000L);

			InterventionStats stats = service.getInterventionStats(
					LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

			assertThat(stats.getTotal()).isEqualTo(8L);
			assertThat(stats.getEnCours()).isEqualTo(3L);
			assertThat(stats.getVehiculesConcernes()).isEqualTo(5L);
			assertThat(stats.getCoutTotal()).isEqualTo(450_000L);
		}

		@Test
		@DisplayName("les statistiques de livraisons fournisseur séparent facturées et non facturées")
		void livraisonFournisseurStats() {
			LocalDateTime debut = LocalDateTime.of(2026, 3, 1, 0, 0);
			LocalDateTime fin = LocalDateTime.of(2026, 4, 1, 0, 0);
			when(livraisonFournisseurRepository.countValidesBetween(debut, fin)).thenReturn(10L);
			when(livraisonFournisseurRepository.countAvecFacture(debut, fin)).thenReturn(6L);
			when(livraisonFournisseurRepository.countSansFacture(debut, fin)).thenReturn(4L);
			when(entreeProduitRepository.sumQuantiteBetween(debut, fin)).thenReturn(250L);

			LivraisonFournisseurStats stats = service.getLivraisonFournisseurStats(
					LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

			assertThat(stats.getTotal()).isEqualTo(10L);
			assertThat(stats.getAvecFacture()).isEqualTo(6L);
			assertThat(stats.getSansFacture()).isEqualTo(4L);
			assertThat(stats.getProduitsEntres()).isEqualTo(250L);
		}

		@Test
		@DisplayName("les statistiques de missions bornent également la période")
		void missionStats() {
			LocalDateTime debut = LocalDateTime.of(2026, 3, 1, 0, 0);
			LocalDateTime fin = LocalDateTime.of(2026, 4, 1, 0, 0);
			when(missionRepository.countByCreatedAtBetween(debut, fin)).thenReturn(12L);
			when(missionRepository.countEnCours(debut, fin)).thenReturn(4L);

			MissionStats stats = service.getMissionStats(
					LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

			assertThat(stats.getTotal()).isEqualTo(12L);
			assertThat(stats.getEnCours()).isEqualTo(4L);
		}
	}
}
