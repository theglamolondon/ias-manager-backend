package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.Facture;
import net.ivoireautoservice.ias_manager.dto.core.Mission;
import net.ivoireautoservice.ias_manager.dto.core.SimulationTarif;
import net.ivoireautoservice.ias_manager.dto.request.AffecterChauffeurRequest;
import net.ivoireautoservice.ias_manager.dto.request.AnnulerMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.ChangerVehiculeMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.FactureRequest;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.enums.*;
import net.ivoireautoservice.ias_manager.event.MissionCreatedEvent;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.DepenseMissionMapper;
import net.ivoireautoservice.ias_manager.mapper.FactureMapper;
import net.ivoireautoservice.ias_manager.mapper.MediaMapper;
import net.ivoireautoservice.ias_manager.mapper.MissionMapper;
import net.ivoireautoservice.ias_manager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MissionService — tarification, cycle de vie et facturation des missions")
class MissionServiceTest {

	@Mock private MissionRepository missionRepository;
	@Mock private DepenseMissionRepository depenseMissionRepository;
	@Mock private PhotoMissionRepository photoMissionRepository;
	@Mock private VehiculeRepository vehiculeRepository;
	@Mock private ChauffeurRepository chauffeurRepository;
	@Mock private PartenaireRepository partenaireRepository;
	@Mock private TypeDepenseRepository typeDepenseRepository;
	@Mock private FactureRepository factureRepository;
	@Mock private LigneCompteRepository ligneCompteRepository;
	@Mock private InterventionRepository interventionRepository;
	@Mock private MissionMapper missionMapper;
	@Mock private FactureMapper factureMapper;
	@Mock private DepenseMissionMapper depenseMissionMapper;
	@Mock private MediaMapper mediaMapper;
	@Mock private MediaService mediaService;
	@Mock private FactureService factureService;
	@Mock private CompteService compteService;
	@Mock private SiteService siteService;
	@Mock private PrintService printService;
	@Mock private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private MissionService service;

	private static final LocalDateTime DEBUT = LocalDateTime.of(2026, 3, 1, 8, 0);

	private VehiculeEntity vehicule;

	@BeforeEach
	void setUp() {
		vehicule = VehiculeEntity.builder()
				.id(1L).immatriculation("AB-123-CD").statut(VehiculeStatusEnum.DISPONIBLE)
				.type(TypeVehiculeEntity.builder().id(1L)
						.prixJournalier(BigDecimal.valueOf(25_000))
						.prixMensuel(BigDecimal.valueOf(600_000)).build())
				.build();
		when(siteService.getSupplementJournalier(any())).thenReturn(BigDecimal.ZERO);
		when(missionRepository.countByYear(anyInt())).thenReturn(0L);
		when(missionMapper.toDto(any(MissionEntity.class))).thenReturn(Mission.builder().build());
	}

	private static int anyInt() {
		return org.mockito.ArgumentMatchers.anyInt();
	}

	private MissionEntity capturerCreation(MissionRequest request) {
		MissionEntity entity = MissionEntity.builder()
				.typeTarification(request.getTypeTarification())
				.dhmsDebutPrevi(request.getDhmsDebutPrevi())
				.dhmsFinPrevi(request.getDhmsFinPrevi())
				.localisation(request.getLocalisation())
				.tarif(request.getTarif())
				.build();
		when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
		when(missionMapper.toEntity(request)).thenReturn(entity);
		when(missionRepository.save(entity)).thenReturn(entity);
		service.createMission(request);
		return entity;
	}

	private static MissionRequest.MissionRequestBuilder requete() {
		return MissionRequest.builder()
				.vehiculeId(1L)
				.typeTarification(TypeTarificationEnum.JOURNALIERE)
				.dhmsDebutPrevi(DEBUT)
				.dhmsFinPrevi(DEBUT.plusDays(2))
				.tarif(BigDecimal.valueOf(30_000));
	}

	@Nested
	@DisplayName("Calcul des champs dérivés à la création")
	class ChampsDerives {

		@Test
		@DisplayName("JOURNALIERE : durée inclusive du jour de départ, montant = tarif × jours")
		void journaliere() {
			MissionEntity entity = capturerCreation(requete().build());

			assertThat(entity.getDureeLocation()).isEqualTo(3L);
			assertThat(entity.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.valueOf(90_000));
		}

		@Test
		@DisplayName("JOURNALIERE : une mission d'un seul jour compte 1 jour")
		void journaliereUnJour() {
			MissionEntity entity = capturerCreation(requete().dhmsFinPrevi(DEBUT.plusHours(6)).build());

			assertThat(entity.getDureeLocation()).isEqualTo(1L);
			assertThat(entity.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
		}

		@Test
		@DisplayName("MENSUELLE : la durée est en mois entamés, avec un plancher à 1")
		void mensuelleMoinsDunMois() {
			MissionEntity entity = capturerCreation(requete()
					.typeTarification(TypeTarificationEnum.MENSUELLE)
					.dhmsFinPrevi(DEBUT.plusDays(20))
					.tarif(BigDecimal.valueOf(700_000))
					.build());

			assertThat(entity.getDureeLocation()).isEqualTo(1L);
			assertThat(entity.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.valueOf(700_000));
		}

		@Test
		@DisplayName("MENSUELLE : 65 jours donnent 2 mois facturés")
		void mensuelleDeuxMois() {
			MissionEntity entity = capturerCreation(requete()
					.typeTarification(TypeTarificationEnum.MENSUELLE)
					.dhmsFinPrevi(DEBUT.plusDays(64))
					.tarif(BigDecimal.valueOf(700_000))
					.build());

			assertThat(entity.getDureeLocation()).isEqualTo(2L);
			assertThat(entity.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.valueOf(1_400_000));
		}

		@Test
		@DisplayName("UNIQUE : le montant est le forfait, indépendant de la durée")
		void unique() {
			MissionEntity entity = capturerCreation(requete()
					.typeTarification(TypeTarificationEnum.UNIQUE)
					.dhmsFinPrevi(DEBUT.plusDays(9))
					.tarif(BigDecimal.valueOf(150_000))
					.build());

			assertThat(entity.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
			assertThat(entity.getDureeLocation()).isEqualTo(10L);
		}

		@Test
		@DisplayName("INDEFINIE sans date de fin : ni durée ni montant calculés (facturation manuelle ultérieure)")
		void indefinieSansFin() {
			MissionEntity entity = capturerCreation(requete()
					.typeTarification(TypeTarificationEnum.INDEFINIE)
					.dhmsFinPrevi(null)
					.tarif(BigDecimal.valueOf(500_000))
					.build());

			assertThat(entity.getDureeLocation()).isNull();
			assertThat(entity.getMontantTotalHT()).isNull();
		}

		@Test
		@DisplayName("INDEFINIE avec dates : durée en mois entamés, plancher à 1")
		void indefinieAvecDates() {
			MissionEntity entity = capturerCreation(requete()
					.typeTarification(TypeTarificationEnum.INDEFINIE)
					.dhmsFinPrevi(DEBUT.plusDays(64))
					.tarif(BigDecimal.valueOf(500_000))
					.build());

			assertThat(entity.getDureeLocation()).isEqualTo(2L);
			assertThat(entity.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
		}

		@Test
		@DisplayName("le code mission est généré au format {année}-{séquence sur 3 chiffres}")
		void codeMission() {
			when(missionRepository.countByYear(anyInt())).thenReturn(4L);

			MissionEntity entity = capturerCreation(requete().build());

			assertThat(entity.getCodeMission())
					.isEqualTo(java.time.LocalDate.now().getYear() + "-005");
		}

		@Test
		@DisplayName("les champs de réalisation ne sont pas renseignés à la création")
		void champsReelsVides() {
			MissionEntity entity = capturerCreation(requete().build());

			assertThat(entity.getDhmsDebutReel()).isNull();
			assertThat(entity.getDhmsFinReel()).isNull();
			assertThat(entity.getKilometrageArrive()).isNull();
		}
	}

	@Nested
	@DisplayName("Tarif minimum")
	class TarifMinimum {

		@Test
		@DisplayName("JOURNALIERE : un tarif sous le prix journalier du type est refusé")
		void journaliereSousMinimum() {
			MissionRequest request = requete().tarif(BigDecimal.valueOf(20_000)).build();
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(missionMapper.toEntity(request)).thenReturn(MissionEntity.builder()
					.typeTarification(TypeTarificationEnum.JOURNALIERE)
					.tarif(BigDecimal.valueOf(20_000)).build());

			assertThatThrownBy(() -> service.createMission(request))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("inférieur au tarif minimum");
			verify(missionRepository, never()).save(any());
		}

		@Test
		@DisplayName("JOURNALIERE : le supplément de localisation relève le minimum exigé")
		void supplementLocalisation() {
			when(siteService.getSupplementJournalier(LocalisationMissionEnum.EXTERIEUR))
					.thenReturn(BigDecimal.valueOf(15_000));
			MissionRequest request = requete()
					.localisation(LocalisationMissionEnum.EXTERIEUR)
					.tarif(BigDecimal.valueOf(35_000)).build();
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(missionMapper.toEntity(request)).thenReturn(MissionEntity.builder()
					.typeTarification(TypeTarificationEnum.JOURNALIERE)
					.localisation(LocalisationMissionEnum.EXTERIEUR)
					.tarif(BigDecimal.valueOf(35_000)).build());

			assertThatThrownBy(() -> service.createMission(request))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("40000");
		}

		@Test
		@DisplayName("MENSUELLE : le minimum est le prix mensuel, sans majoration de localisation")
		void mensuelleSousMinimum() {
			MissionRequest request = requete()
					.typeTarification(TypeTarificationEnum.MENSUELLE)
					.localisation(LocalisationMissionEnum.EXTERIEUR)
					.tarif(BigDecimal.valueOf(500_000)).build();
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(missionMapper.toEntity(request)).thenReturn(MissionEntity.builder()
					.typeTarification(TypeTarificationEnum.MENSUELLE)
					.localisation(LocalisationMissionEnum.EXTERIEUR)
					.tarif(BigDecimal.valueOf(500_000)).build());

			assertThatThrownBy(() -> service.createMission(request))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("mois");
		}

		@Test
		@DisplayName("UNIQUE et INDEFINIE ne sont pas soumis au tarif minimum")
		void forfaitsNonContraints() {
			MissionEntity entity = capturerCreation(requete()
					.typeTarification(TypeTarificationEnum.UNIQUE)
					.tarif(BigDecimal.ONE).build());

			assertThat(entity.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.ONE);
		}

		@Test
		@DisplayName("un véhicule sans type n'a pas de minimum calculable : le tarif passe")
		void vehiculeSansType() {
			vehicule.setType(null);

			MissionEntity entity = capturerCreation(requete().tarif(BigDecimal.ONE).build());

			assertThat(entity.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.valueOf(3));
		}

		@Test
		@DisplayName("un tarif nul est refusé quand un minimum est défini")
		void tarifNull() {
			MissionRequest request = requete().tarif(null).build();
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(missionMapper.toEntity(request)).thenReturn(MissionEntity.builder()
					.typeTarification(TypeTarificationEnum.JOURNALIERE).build());

			assertThatThrownBy(() -> service.createMission(request))
					.isInstanceOf(BadRequestException.class);
		}
	}

	@Nested
	@DisplayName("Validations de création")
	class ValidationsCreation {

		@Test
		@DisplayName("une mission sous-traitée exige les détails du véhicule")
		void sousTraitanceSansDetails() {
			MissionRequest request = requete().isSousTraitee(true).build();

			assertThatThrownBy(() -> service.createMission(request))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("sous-traitance");
			verifyNoInteractions(vehiculeRepository);
		}

		@Test
		@DisplayName("un véhicule inconnu lève 404")
		void vehiculeInconnu() {
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createMission(requete().build()))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Véhicule avec l'id 1");
		}

		@Test
		@DisplayName("un chauffeur inconnu lève 404 quand la mission est avec chauffeur")
		void chauffeurInconnu() {
			MissionRequest request = requete().withChauffeur(true).chauffeurId(99L).build();
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(missionMapper.toEntity(request)).thenReturn(new MissionEntity());
			when(chauffeurRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createMission(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Chauffeur avec l'id 99");
		}

		@Test
		@DisplayName("un événement de création est publié après enregistrement")
		void evenementPublie() {
			capturerCreation(requete().build());

			ArgumentCaptor<MissionCreatedEvent> captor = ArgumentCaptor.forClass(MissionCreatedEvent.class);
			verify(eventPublisher).publishEvent(captor.capture());
			assertThat(captor.getValue().immatriculation()).isEqualTo("AB-123-CD");
		}
	}

	@Nested
	@DisplayName("Facturation automatique à la création")
	class FacturationAutomatique {

		private MissionRequest requeteAvecClient(TypeTarificationEnum type, Boolean genererFacture, Long compteId) {
			return requete().typeTarification(type).clientId(5L)
					.genererFacture(genererFacture).compteId(compteId).build();
		}

		private void stub(MissionRequest request, PartenaireEntity client) {
			MissionEntity entity = MissionEntity.builder()
					.id(1L).typeTarification(request.getTypeTarification())
					.dhmsDebutPrevi(request.getDhmsDebutPrevi())
					.dhmsFinPrevi(request.getDhmsFinPrevi())
					.tarif(request.getTarif()).build();
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(missionMapper.toEntity(request)).thenReturn(entity);
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.save(entity)).thenReturn(entity);
		}

		@Test
		@DisplayName("une mission JOURNALIERE avec client génère une facture MISSION")
		void factureGeneree() {
			PartenaireEntity client = PartenaireEntity.builder().id(5L).raisonSociale("Client").build();
			MissionRequest request = requeteAvecClient(TypeTarificationEnum.JOURNALIERE, null, null);
			stub(request, client);
			when(factureService.createFacture(any(FactureRequest.class), any(FactureTypeEnum.class)))
					.thenReturn(Facture.builder().id(9L).build());

			service.createMission(request);

			ArgumentCaptor<FactureRequest> captor = ArgumentCaptor.forClass(FactureRequest.class);
			verify(factureService).createFacture(captor.capture(),
					org.mockito.ArgumentMatchers.eq(FactureTypeEnum.MISSION));
			FactureRequest facture = captor.getValue();
			assertThat(facture.getFactureClient()).isTrue();
			assertThat(facture.getPartenaireId()).isEqualTo(5L);
			assertThat(facture.getMontantHt()).isEqualTo(90_000L);
			assertThat(facture.getMontantTtc()).isEqualTo(90_000L);
			assertThat(facture.getItems()).hasSize(1);
			assertThat(facture.getItems().get(0).getExtraRef()).isNotBlank();
		}

		@Test
		@DisplayName("une mission INDEFINIE n'est pas auto-facturée")
		void indefinieNonFacturee() {
			PartenaireEntity client = PartenaireEntity.builder().id(5L).build();
			MissionRequest request = requeteAvecClient(TypeTarificationEnum.INDEFINIE, null, null);
			stub(request, client);

			service.createMission(request);

			verify(factureService, never()).createFacture(any(), any());
		}

		@Test
		@DisplayName("genererFacture=false désactive la facturation automatique")
		void desactivationExplicite() {
			PartenaireEntity client = PartenaireEntity.builder().id(5L).build();
			MissionRequest request = requeteAvecClient(TypeTarificationEnum.JOURNALIERE, false, null);
			stub(request, client);

			service.createMission(request);

			verify(factureService, never()).createFacture(any(), any());
		}

		@Test
		@DisplayName("une mission sans client n'est pas facturée")
		void sansClient() {
			capturerCreation(requete().build());

			verify(factureService, never()).createFacture(any(), any());
		}

		@Test
		@DisplayName("un compte fourni marque immédiatement la facture comme payée")
		void paiementImmediat() {
			PartenaireEntity client = PartenaireEntity.builder().id(5L).build();
			MissionRequest request = requeteAvecClient(TypeTarificationEnum.JOURNALIERE, null, 3L);
			stub(request, client);
			when(factureService.createFacture(any(FactureRequest.class), any(FactureTypeEnum.class)))
					.thenReturn(Facture.builder().id(9L).build());

			service.createMission(request);

			verify(factureService).changerStatut(9L, FactureStatusEnum.PAYEE, 3L);
		}
	}

	@Nested
	@DisplayName("Démarrage et clôture")
	class DemarrageCloture {

		private MissionEntity mission(VehiculeEntity vehicule, ChauffeurEntity chauffeur, boolean withChauffeur) {
			return MissionEntity.builder()
					.id(1L).codeMission("2026-001").vehicule(vehicule)
					.chauffeur(chauffeur).withChauffeur(withChauffeur).build();
		}

		private ChauffeurEntity chauffeur(StatutChauffeurEnum statut) {
			return ChauffeurEntity.builder().id(2L).statut(statut)
					.employe(EmployeEntity.builder().id(3L).nom("Kouassi").build()).build();
		}

		@Test
		@DisplayName("démarrer passe le véhicule et le chauffeur en MISSION")
		void demarrage() {
			ChauffeurEntity chauffeur = chauffeur(StatutChauffeurEnum.DISPONIBLE);
			MissionEntity entity = mission(vehicule, chauffeur, true);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.demarrerMission(1L, DEBUT);

			assertThat(entity.getDhmsDebutReel()).isEqualTo(DEBUT);
			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.MISSION);
			assertThat(chauffeur.getStatut()).isEqualTo(StatutChauffeurEnum.MISSION);
		}

		@Test
		@DisplayName("une mission annulée ne peut pas démarrer")
		void missionAnnulee() {
			MissionEntity entity = mission(vehicule, null, false);
			entity.setDhmsAnnulation(LocalDateTime.now());
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.demarrerMission(1L, DEBUT))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("annulée");
		}

		@Test
		@DisplayName("une mission déjà démarrée ne peut pas l'être à nouveau")
		void dejaDemarree() {
			MissionEntity entity = mission(vehicule, null, false);
			entity.setDhmsDebutReel(DEBUT);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.demarrerMission(1L, DEBUT))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà été démarrée");
		}

		@Test
		@DisplayName("une mission avec chauffeur ne démarre pas sans chauffeur affecté")
		void chauffeurManquant() {
			MissionEntity entity = mission(vehicule, null, true);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.demarrerMission(1L, DEBUT))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("affectez un chauffeur");
		}

		@Test
		@DisplayName("un véhicule non disponible bloque le démarrage")
		void vehiculeIndisponible() {
			vehicule.setStatut(VehiculeStatusEnum.GARAGE);
			MissionEntity entity = mission(vehicule, null, false);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.demarrerMission(1L, DEBUT))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'est pas disponible");
		}

		@Test
		@DisplayName("un chauffeur non disponible bloque le démarrage")
		void chauffeurIndisponible() {
			ChauffeurEntity chauffeur = chauffeur(StatutChauffeurEnum.MISSION);
			MissionEntity entity = mission(vehicule, chauffeur, true);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.demarrerMission(1L, DEBUT))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'est pas disponible");
		}

		@Test
		@DisplayName("terminer restitue le véhicule DISPONIBLE et libère le chauffeur")
		void cloture() {
			vehicule.setStatut(VehiculeStatusEnum.MISSION);
			ChauffeurEntity chauffeur = chauffeur(StatutChauffeurEnum.MISSION);
			MissionEntity entity = mission(vehicule, chauffeur, true);
			entity.setDhmsDebutReel(DEBUT);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(missionRepository.save(entity)).thenReturn(entity);
			when(interventionRepository.existsByVehiculeIdAndStatut(1L, InterventionStatut.EN_COURS))
					.thenReturn(false);

			service.terminerMission(1L, DEBUT.plusDays(2));

			assertThat(entity.getDhmsFinReel()).isEqualTo(DEBUT.plusDays(2));
			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.DISPONIBLE);
			assertThat(chauffeur.getStatut()).isEqualTo(StatutChauffeurEnum.DISPONIBLE);
		}

		@Test
		@DisplayName("un véhicule avec une intervention en cours revient au GARAGE")
		void clotureVersGarage() {
			vehicule.setStatut(VehiculeStatusEnum.MISSION);
			MissionEntity entity = mission(vehicule, null, false);
			entity.setDhmsDebutReel(DEBUT);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(missionRepository.save(entity)).thenReturn(entity);
			when(interventionRepository.existsByVehiculeIdAndStatut(1L, InterventionStatut.EN_COURS))
					.thenReturn(true);

			service.terminerMission(1L, DEBUT.plusDays(2));

			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.GARAGE);
		}

		@Test
		@DisplayName("un véhicule dont le statut a changé en externe n'est pas écrasé à la clôture")
		void statutExterneConserve() {
			vehicule.setStatut(VehiculeStatusEnum.SINISTRE);
			MissionEntity entity = mission(vehicule, null, false);
			entity.setDhmsDebutReel(DEBUT);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.terminerMission(1L, DEBUT.plusDays(2));

			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.SINISTRE);
			verify(vehiculeRepository, never()).save(any());
		}

		@Test
		@DisplayName("une mission non démarrée ne peut pas être terminée")
		void clotureSansDemarrage() {
			MissionEntity entity = mission(vehicule, null, false);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.terminerMission(1L, DEBUT))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("pas encore été démarrée");
		}

		@Test
		@DisplayName("une mission déjà terminée ne peut pas l'être à nouveau")
		void dejaTerminee() {
			MissionEntity entity = mission(vehicule, null, false);
			entity.setDhmsDebutReel(DEBUT);
			entity.setDhmsFinReel(DEBUT.plusDays(1));
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.terminerMission(1L, DEBUT.plusDays(2)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà terminée");
		}
	}

	@Nested
	@DisplayName("Affectation de chauffeur")
	class AffectationChauffeur {

		@Test
		@DisplayName("affecter un chauffeur disponible active withChauffeur")
		void affectation() {
			ChauffeurEntity chauffeur = ChauffeurEntity.builder().id(2L)
					.statut(StatutChauffeurEnum.DISPONIBLE)
					.employe(EmployeEntity.builder().nom("Kouassi").build()).build();
			MissionEntity entity = MissionEntity.builder().id(1L).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(chauffeurRepository.findById(2L)).thenReturn(Optional.of(chauffeur));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.affecterChauffeur(1L, AffecterChauffeurRequest.builder().chauffeurId(2L).build());

			assertThat(entity.getChauffeur()).isSameAs(chauffeur);
			assertThat(entity.getWithChauffeur()).isTrue();
		}

		@Test
		@DisplayName("affecter null retire le chauffeur et désactive withChauffeur")
		void desaffectation() {
			MissionEntity entity = MissionEntity.builder().id(1L)
					.chauffeur(ChauffeurEntity.builder().id(2L).build()).withChauffeur(true).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.affecterChauffeur(1L, AffecterChauffeurRequest.builder().build());

			assertThat(entity.getChauffeur()).isNull();
			assertThat(entity.getWithChauffeur()).isFalse();
		}

		@Test
		@DisplayName("un chauffeur non disponible est refusé")
		void chauffeurIndisponible() {
			ChauffeurEntity chauffeur = ChauffeurEntity.builder().id(2L)
					.statut(StatutChauffeurEnum.MISSION)
					.employe(EmployeEntity.builder().nom("Kouassi").build()).build();
			MissionEntity entity = MissionEntity.builder().id(1L).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(chauffeurRepository.findById(2L)).thenReturn(Optional.of(chauffeur));

			assertThatThrownBy(() -> service.affecterChauffeur(1L,
					AffecterChauffeurRequest.builder().chauffeurId(2L).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'est pas disponible");
		}

		@Test
		@DisplayName("une mission déjà démarrée n'accepte plus de réaffectation")
		void missionDemarree() {
			MissionEntity entity = MissionEntity.builder().id(1L).dhmsDebutReel(DEBUT).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.affecterChauffeur(1L,
					AffecterChauffeurRequest.builder().chauffeurId(2L).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà démarré");
		}

		@Test
		@DisplayName("une mission annulée n'accepte plus de réaffectation")
		void missionAnnulee() {
			MissionEntity entity = MissionEntity.builder().id(1L)
					.dhmsAnnulation(LocalDateTime.now()).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.affecterChauffeur(1L,
					AffecterChauffeurRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("annulée");
		}
	}

	@Nested
	@DisplayName("Simulation de tarif")
	class Simulation {

		@Test
		@DisplayName("JOURNALIERE : minimum = (prix journalier + supplément) × jours")
		void journaliere() {
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(siteService.getSupplementJournalier(LocalisationMissionEnum.INTERIEUR))
					.thenReturn(BigDecimal.valueOf(10_000));

			SimulationTarif simulation = service.simulerTarif(1L, TypeTarificationEnum.JOURNALIERE,
					DEBUT, DEBUT.plusDays(2), LocalisationMissionEnum.INTERIEUR);

			assertThat(simulation.getDuree()).isEqualTo(3L);
			assertThat(simulation.getTarifUnitaire()).isEqualByComparingTo(BigDecimal.valueOf(35_000));
			assertThat(simulation.getTarifMinimum()).isEqualByComparingTo(BigDecimal.valueOf(105_000));
		}

		@Test
		@DisplayName("MENSUELLE : aucune majoration de localisation, durée en mois")
		void mensuelle() {
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));

			SimulationTarif simulation = service.simulerTarif(1L, TypeTarificationEnum.MENSUELLE,
					DEBUT, DEBUT.plusDays(64), LocalisationMissionEnum.EXTERIEUR);

			assertThat(simulation.getDuree()).isEqualTo(2L);
			assertThat(simulation.getTarifUnitaire()).isEqualByComparingTo(BigDecimal.valueOf(600_000));
			assertThat(simulation.getTarifMinimum()).isEqualByComparingTo(BigDecimal.valueOf(1_200_000));
			verify(siteService, never()).getSupplementJournalier(any());
		}

		@Test
		@DisplayName("UNIQUE et INDEFINIE renvoient une simulation neutre sans lire le véhicule")
		void forfaits() {
			SimulationTarif unique = service.simulerTarif(1L, TypeTarificationEnum.UNIQUE, DEBUT, DEBUT, null);
			SimulationTarif indefinie = service.simulerTarif(1L, TypeTarificationEnum.INDEFINIE, DEBUT, DEBUT, null);

			assertThat(unique.getDuree()).isZero();
			assertThat(unique.getTarifMinimum()).isNull();
			assertThat(indefinie.getTarifUnitaire()).isNull();
			verify(vehiculeRepository, never()).findById(any());
		}

		@Test
		@DisplayName("un véhicule sans type ne peut pas être simulé")
		void vehiculeSansType() {
			vehicule.setType(null);
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));

			assertThatThrownBy(() -> service.simulerTarif(1L, TypeTarificationEnum.JOURNALIERE,
					DEBUT, DEBUT.plusDays(1), null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("pas de type défini");
		}

		@Test
		@DisplayName("un prix de référence absent est traité comme zéro")
		void prixAbsent() {
			vehicule.getType().setPrixJournalier(null);
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));

			SimulationTarif simulation = service.simulerTarif(1L, TypeTarificationEnum.JOURNALIERE,
					DEBUT, DEBUT, null);

			assertThat(simulation.getTarifMinimum()).isEqualByComparingTo(BigDecimal.ZERO);
		}
	}

	@Nested
	@DisplayName("Annulation")
	class Annulation {

		private MissionEntity mission() {
			return MissionEntity.builder().id(1L).codeMission("2026-001").build();
		}

		@Test
		@DisplayName("une mission démarrée ne peut plus être annulée")
		void missionDemarree() {
			MissionEntity entity = mission();
			entity.setDhmsDebutReel(DEBUT);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.annulerMission(1L, AnnulerMissionRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà démarré");
		}

		@Test
		@DisplayName("une mission déjà annulée ne peut pas l'être à nouveau")
		void dejaAnnulee() {
			MissionEntity entity = mission();
			entity.setDhmsAnnulation(LocalDateTime.now());
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.annulerMission(1L, AnnulerMissionRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà annulée");
		}

		@Test
		@DisplayName("un reçu annulé déclenche un remboursement sur le compte d'encaissement d'origine")
		void recuRembourse() {
			MissionEntity entity = mission();
			FactureEntity facture = FactureEntity.builder().id(9L).numProforma("DA/01/79/1")
					.nature(FactureNatureEnum.RECU).statut(FactureStatusEnum.PAYEE).build();
			LigneCompteEntity encaissement = LigneCompteEntity.builder()
					.id(1L).compte(CompteEntity.builder().id(4L).build()).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));
			when(ligneCompteRepository.findFirstByFactureIdAndTypeOrderByDhmsOperationAsc(
					9L, CompteLigneType.APPROVISIONNEMENT)).thenReturn(Optional.of(encaissement));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.annulerMission(1L, AnnulerMissionRequest.builder().motif("Client absent").build());

			verify(factureService).enregistrerRemboursement(facture, 4L);
			assertThat(facture.getStatut()).isEqualTo(FactureStatusEnum.ANNULEE);
			assertThat(entity.getDhmsAnnulation()).isNotNull();
			assertThat(entity.getMotifAnnulation()).isEqualTo("Client absent");
		}

		@Test
		@DisplayName("un reçu dont l'encaissement est introuvable bloque l'annulation")
		void recuSansEncaissement() {
			MissionEntity entity = mission();
			FactureEntity facture = FactureEntity.builder().id(9L).numProforma("DA/01/79/1")
					.nature(FactureNatureEnum.RECU).statut(FactureStatusEnum.PAYEE).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));
			when(ligneCompteRepository.findFirstByFactureIdAndTypeOrderByDhmsOperationAsc(any(), any()))
					.thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.annulerMission(1L, AnnulerMissionRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("remboursement manuellement");
		}

		@Test
		@DisplayName("une facture non payée génère un avoir sans remboursement")
		void factureNonPayee() {
			MissionEntity entity = mission();
			FactureEntity facture = FactureEntity.builder().id(9L)
					.nature(FactureNatureEnum.FACTURE).statut(FactureStatusEnum.PROFORMA).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.annulerMission(1L, AnnulerMissionRequest.builder().build());

			verify(factureService).genererAvoir(facture);
			verify(factureService, never()).enregistrerRemboursement(any(), any());
			assertThat(facture.getStatut()).isEqualTo(FactureStatusEnum.ANNULEE);
		}

		@Test
		@DisplayName("une facture payée exige un compte de remboursement")
		void facturePayeeSansCompte() {
			MissionEntity entity = mission();
			FactureEntity facture = FactureEntity.builder().id(9L)
					.nature(FactureNatureEnum.FACTURE).statut(FactureStatusEnum.PAYEE).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));

			assertThatThrownBy(() -> service.annulerMission(1L, AnnulerMissionRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("compte de remboursement est obligatoire");
		}

		@Test
		@DisplayName("une facture payée avec compte génère avoir puis remboursement")
		void facturePayeeAvecCompte() {
			MissionEntity entity = mission();
			FactureEntity facture = FactureEntity.builder().id(9L)
					.nature(FactureNatureEnum.FACTURE).statut(FactureStatusEnum.PAYEE).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.annulerMission(1L, AnnulerMissionRequest.builder().compteId(4L).build());

			verify(factureService).genererAvoir(facture);
			verify(factureService).enregistrerRemboursement(facture, 4L);
		}

		@Test
		@DisplayName("une facture déjà annulée bloque l'annulation de la mission")
		void factureDejaAnnulee() {
			MissionEntity entity = mission();
			FactureEntity facture = FactureEntity.builder().id(9L)
					.nature(FactureNatureEnum.FACTURE).statut(FactureStatusEnum.ANNULEE).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));

			assertThatThrownBy(() -> service.annulerMission(1L, AnnulerMissionRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà annulée");
		}

		@Test
		@DisplayName("une facture qui est déjà un avoir bloque l'annulation")
		void factureAvoir() {
			MissionEntity entity = mission();
			FactureEntity facture = FactureEntity.builder().id(9L)
					.nature(FactureNatureEnum.AVOIR).statut(FactureStatusEnum.FACTUREE).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));

			assertThatThrownBy(() -> service.annulerMission(1L, AnnulerMissionRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà un avoir");
		}
	}

	@Nested
	@DisplayName("Changement de véhicule en cours de mission")
	class ChangementVehicule {

		private MissionEntity missionEnCours(TypeTarificationEnum type, LocalDateTime finPrevi) {
			return MissionEntity.builder()
					.id(1L).codeMission("2026-001").typeTarification(type)
					.vehicule(vehicule).dhmsDebutReel(LocalDateTime.now().minusDays(4))
					.dhmsFinPrevi(finPrevi).tarif(BigDecimal.valueOf(30_000)).build();
		}

		private VehiculeEntity nouveauVehicule(VehiculeStatusEnum statut) {
			return VehiculeEntity.builder().id(2L).immatriculation("EF-456-GH").statut(statut).build();
		}

		@Test
		@DisplayName("l'ancienne mission est clôturée et refacturée sur la durée réelle")
		void ancienneMissionRefacturee() {
			vehicule.setStatut(VehiculeStatusEnum.MISSION);
			MissionEntity ancienne = missionEnCours(TypeTarificationEnum.JOURNALIERE,
					LocalDateTime.now().plusDays(6));
			VehiculeEntity nouveau = nouveauVehicule(VehiculeStatusEnum.DISPONIBLE);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(ancienne));
			when(vehiculeRepository.findById(2L)).thenReturn(Optional.of(nouveau));
			when(missionRepository.save(any(MissionEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.changerVehicule(ChangerVehiculeMissionRequest.builder()
					.missionId(1L).nouveauVehiculeId(2L)
					.nouveauStatutAncienVehicule(VehiculeStatusEnum.SINISTRE).build());

			assertThat(ancienne.getDhmsFinReel()).isNotNull();
			assertThat(ancienne.getDureeLocation()).isEqualTo(4L);
			assertThat(ancienne.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.valueOf(120_000));
			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.SINISTRE);
			assertThat(nouveau.getStatut()).isEqualTo(VehiculeStatusEnum.MISSION);
		}

		@Test
		@DisplayName("un statut cible incohérent pour l'ancien véhicule retombe sur GARAGE")
		void statutAncienVehiculeParDefaut() {
			vehicule.setStatut(VehiculeStatusEnum.MISSION);
			MissionEntity ancienne = missionEnCours(TypeTarificationEnum.JOURNALIERE,
					LocalDateTime.now().plusDays(6));
			when(missionRepository.findById(1L)).thenReturn(Optional.of(ancienne));
			when(vehiculeRepository.findById(2L)).thenReturn(Optional.of(nouveauVehicule(VehiculeStatusEnum.DISPONIBLE)));
			when(missionRepository.save(any(MissionEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.changerVehicule(ChangerVehiculeMissionRequest.builder()
					.missionId(1L).nouveauVehiculeId(2L)
					.nouveauStatutAncienVehicule(VehiculeStatusEnum.DISPONIBLE).build());

			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.GARAGE);
		}

		@Test
		@DisplayName("un forfait UNIQUE déjà facturé donne une nouvelle mission à montant nul")
		void forfaitReporteAZero() {
			vehicule.setStatut(VehiculeStatusEnum.MISSION);
			MissionEntity ancienne = missionEnCours(TypeTarificationEnum.UNIQUE,
					LocalDateTime.now().plusDays(6));
			when(missionRepository.findById(1L)).thenReturn(Optional.of(ancienne));
			when(vehiculeRepository.findById(2L)).thenReturn(Optional.of(nouveauVehicule(VehiculeStatusEnum.DISPONIBLE)));
			ArgumentCaptor<MissionEntity> captor = ArgumentCaptor.forClass(MissionEntity.class);
			when(missionRepository.save(any(MissionEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.changerVehicule(ChangerVehiculeMissionRequest.builder()
					.missionId(1L).nouveauVehiculeId(2L)
					.nouveauStatutAncienVehicule(VehiculeStatusEnum.GARAGE).build());

			verify(missionRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
			MissionEntity nouvelle = captor.getAllValues().get(captor.getAllValues().size() - 1);
			assertThat(nouvelle.getMontantTotalHT()).isEqualByComparingTo(BigDecimal.ZERO);
			assertThat(nouvelle.getVehicule().getId()).isEqualTo(2L);
		}

		@Test
		@DisplayName("une mission non démarrée ne peut pas changer de véhicule")
		void missionNonDemarree() {
			MissionEntity ancienne = MissionEntity.builder().id(1L).vehicule(vehicule).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(ancienne));

			assertThatThrownBy(() -> service.changerVehicule(ChangerVehiculeMissionRequest.builder()
					.missionId(1L).nouveauVehiculeId(2L).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("pas encore été démarrée");
		}

		@Test
		@DisplayName("une mission terminée ne peut pas changer de véhicule")
		void missionTerminee() {
			MissionEntity ancienne = missionEnCours(TypeTarificationEnum.JOURNALIERE, null);
			ancienne.setDhmsFinReel(LocalDateTime.now());
			when(missionRepository.findById(1L)).thenReturn(Optional.of(ancienne));

			assertThatThrownBy(() -> service.changerVehicule(ChangerVehiculeMissionRequest.builder()
					.missionId(1L).nouveauVehiculeId(2L).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà terminée");
		}

		@Test
		@DisplayName("un véhicule de remplacement non disponible est refusé")
		void nouveauVehiculeIndisponible() {
			MissionEntity ancienne = missionEnCours(TypeTarificationEnum.JOURNALIERE, null);
			when(missionRepository.findById(1L)).thenReturn(Optional.of(ancienne));
			when(vehiculeRepository.findById(2L)).thenReturn(Optional.of(nouveauVehicule(VehiculeStatusEnum.GARAGE)));

			assertThatThrownBy(() -> service.changerVehicule(ChangerVehiculeMissionRequest.builder()
					.missionId(1L).nouveauVehiculeId(2L).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'est pas disponible");
		}
	}

	@Nested
	@DisplayName("Dépenses de mission")
	class Depenses {

		@Test
		@DisplayName("une dépense est imputée en DEPENSE sur le compte fourni")
		void depenseImputee() {
			MissionEntity mission = MissionEntity.builder().id(1L).codeMission("2026-001").build();
			TypeDepenseEntity type = TypeDepenseEntity.builder().id(2L).libelle("Carburant").build();
			var request = net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest.builder()
					.libelle("Plein").montant(25_000L).typeDepenseId(2L).compteId(3L).build();
			DepenseMissionEntity entity = new DepenseMissionEntity();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
			when(typeDepenseRepository.findById(2L)).thenReturn(Optional.of(type));
			when(depenseMissionMapper.toEntity(request)).thenReturn(entity);
			when(depenseMissionRepository.save(entity)).thenReturn(entity);

			service.addDepense(1L, request);

			var captor = ArgumentCaptor.forClass(
					net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest.class);
			verify(compteService).createLigne(org.mockito.ArgumentMatchers.eq(3L), captor.capture());
			assertThat(captor.getValue().getType()).isEqualTo(CompteLigneType.DEPENSE);
			assertThat(captor.getValue().getMontant()).isEqualTo(25_000L);
			assertThat(captor.getValue().getObjet()).contains("2026-001").contains("Carburant").contains("Plein");
			assertThat(entity.getMission()).isSameAs(mission);
			assertThat(entity.getTypeDepense()).isSameAs(type);
		}

		@Test
		@DisplayName("un type de dépense inconnu lève 404")
		void typeInconnu() {
			var request = net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest.builder()
					.montant(1_000L).typeDepenseId(99L).compteId(3L).build();
			when(missionRepository.findById(1L))
					.thenReturn(Optional.of(MissionEntity.builder().id(1L).build()));
			when(typeDepenseRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.addDepense(1L, request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Type de dépense");
		}
	}

	@Nested
	@DisplayName("Ordre de mission (PDF)")
	class OrdreMission {

		@Test
		@DisplayName("refuse de générer un ordre sans chauffeur affecté")
		void sansChauffeur() {
			MissionEntity entity = MissionEntity.builder().id(1L).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.generateOrdreMissionPdf(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Aucun chauffeur");
			verifyNoInteractions(printService);
		}

		@Test
		@DisplayName("génère le PDF quand un chauffeur est affecté")
		void avecChauffeur() {
			MissionEntity entity = MissionEntity.builder().id(1L)
					.chauffeur(ChauffeurEntity.builder().id(2L).build()).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(missionMapper.toDto(entity)).thenReturn(Mission.builder()
					.dhmsDebutPrevi(DEBUT).dhmsFinPrevi(DEBUT.plusDays(2)).build());
			when(printService.generatePdf(any(), any())).thenReturn(new byte[]{1});

			assertThat(service.generateOrdreMissionPdf(1L)).isNotEmpty();
			verify(printService).generatePdf(org.mockito.ArgumentMatchers.eq("pdf/OrdreMission"), any());
		}
	}

	@Nested
	@DisplayName("Modification d'une mission facturée")
	class ModificationFacturee {

		private MissionEntity missionAvecFacture() {
			return MissionEntity.builder()
					.id(1L).codeMission("2026-001").typeTarification(TypeTarificationEnum.JOURNALIERE)
					.dhmsDebutPrevi(DEBUT).dhmsFinPrevi(DEBUT.plusDays(2))
					.tarif(BigDecimal.valueOf(30_000)).vehicule(vehicule)
					.client(PartenaireEntity.builder().id(5L).build()).build();
		}

		@Test
		@DisplayName("une mission annulée n'est plus modifiable")
		void missionAnnulee() {
			MissionEntity entity = missionAvecFacture();
			entity.setDhmsAnnulation(LocalDateTime.now());
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.updateMission(1L, requete().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("annulée");
		}

		@Test
		@DisplayName("un changement comptable est refusé si la facture liée est déjà PAYEE")
		void factureVerrouillee() {
			MissionEntity entity = missionAvecFacture();
			FactureEntity facture = FactureEntity.builder().id(9L).numProforma("DA/01/79/1")
					.nature(FactureNatureEnum.FACTURE).statut(FactureStatusEnum.PAYEE).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));

			assertThatThrownBy(() -> service.updateMission(1L,
					requete().tarif(BigDecimal.valueOf(50_000)).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("interdit toute modification comptable");
		}

		@Test
		@DisplayName("un changement comptable resynchronise une facture encore en PROFORMA")
		void resynchronisation() {
			MissionEntity entity = missionAvecFacture();
			PartenaireEntity client = entity.getClient();
			FactureEntity facture = FactureEntity.builder().id(9L)
					.nature(FactureNatureEnum.FACTURE).statut(FactureStatusEnum.PROFORMA).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.updateMission(1L, requete().clientId(5L).tarif(BigDecimal.valueOf(50_000)).build());

			verify(factureService).replaceMissionFactureLines(
					org.mockito.ArgumentMatchers.eq(facture),
					org.mockito.ArgumentMatchers.eq(client),
					any());
		}

		@Test
		@DisplayName("une modification non comptable ne touche pas à la facture")
		void modificationNonComptable() {
			MissionEntity entity = missionAvecFacture();
			FactureEntity facture = FactureEntity.builder().id(9L)
					.nature(FactureNatureEnum.FACTURE).statut(FactureStatusEnum.PAYEE).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of(facture));
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(entity.getClient()));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.updateMission(1L, requete().clientId(5L).observations("Nouvelle note").build());

			verify(factureService, never()).replaceMissionFactureLines(any(), any(), any());
		}
	}

	@Nested
	@DisplayName("Enregistrement de médias au démarrage")
	class Medias {

		@Test
		@DisplayName("un nombre de types différent du nombre de fichiers est refusé")
		void desynchronisationFichiersTypes() {
			MissionEntity entity = MissionEntity.builder().id(1L).vehicule(vehicule).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(missionRepository.save(entity)).thenReturn(entity);
			var fichier = new org.springframework.mock.web.MockMultipartFile(
					"f", "p.png", "image/png", new byte[]{1});

			assertThatThrownBy(() -> service.demarrerMissionAvecMedias(1L, DEBUT, null,
					List.of(fichier), List.of()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("ne correspond pas au nombre de types");
		}

		@Test
		@DisplayName("une liste de fichiers vide n'entraîne aucun téléversement")
		void aucunFichier() {
			MissionEntity entity = MissionEntity.builder().id(1L).vehicule(vehicule).build();
			when(missionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(missionRepository.save(entity)).thenReturn(entity);

			service.demarrerMissionAvecMedias(1L, DEBUT, null, List.of(), null);

			verifyNoInteractions(mediaService);
			verifyNoInteractions(photoMissionRepository);
		}
	}
}
