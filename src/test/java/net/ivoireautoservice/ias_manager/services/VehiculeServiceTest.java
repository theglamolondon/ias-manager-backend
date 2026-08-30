package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.DocumentVehicule;
import net.ivoireautoservice.ias_manager.dto.core.Media;
import net.ivoireautoservice.ias_manager.dto.core.Vehicule;
import net.ivoireautoservice.ias_manager.dto.core.VehiculeHistorique;
import net.ivoireautoservice.ias_manager.dto.request.VehiculeRequest;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.*;
import net.ivoireautoservice.ias_manager.repository.*;
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
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VehiculeService — registre de flotte, documents et historique")
class VehiculeServiceTest {

	@Mock private VehiculeRepository vehiculeRepository;
	@Mock private TypeVehiculeRepository typeVehiculeRepository;
	@Mock private TypeCarburantRepository typeCarburantRepository;
	@Mock private TypeAssuranceRepository typeAssuranceRepository;
	@Mock private AssuranceRepository assuranceRepository;
	@Mock private TypeInterventionRepository typeInterventionRepository;
	@Mock private InterventionRepository interventionRepository;
	@Mock private MissionRepository missionRepository;
	@Mock private DepenseMissionRepository depenseMissionRepository;
	@Mock private LigneFactureRepository ligneFactureRepository;
	@Mock private DocumentVehiculeRepository documentVehiculeRepository;
	@Mock private MediaRepository mediaRepository;
	@Mock private MarqueRepository marqueRepository;
	@Mock private MediaService mediaService;
	@Mock private SharedService sharedService;
	@Mock private VehiculeMapper vehiculeMapper;
	@Mock private InterventionMapper interventionMapper;
	@Mock private DepenseMissionMapper depenseMissionMapper;
	@Mock private DocumentVehiculeMapper documentVehiculeMapper;

	@InjectMocks
	private VehiculeService service;

	private static VehiculeRequest requete() {
		return VehiculeRequest.builder().immatriculation("AB-123-CD").typeId(1L).build();
	}

	private void stubCreation(VehiculeEntity entity, VehiculeRequest request) {
		when(typeVehiculeRepository.findById(1L))
				.thenReturn(Optional.of(TypeVehiculeEntity.builder().id(1L).build()));
		when(vehiculeMapper.toEntity(request)).thenReturn(entity);
		when(vehiculeRepository.save(entity)).thenReturn(entity);
		when(vehiculeMapper.toDto(entity)).thenReturn(Vehicule.builder().build());
	}

	@Nested
	@DisplayName("Création et mise à jour")
	class CreationMiseAJour {

		@Test
		@DisplayName("un véhicule est créé DISPONIBLE, quel que soit le statut envoyé")
		void statutInitial() {
			VehiculeEntity entity = VehiculeEntity.builder().statut(VehiculeStatusEnum.SINISTRE).build();
			VehiculeRequest request = requete();
			stubCreation(entity, request);

			service.createVehicule(request);

			assertThat(entity.getStatut()).isEqualTo(VehiculeStatusEnum.DISPONIBLE);
		}

		@Test
		@DisplayName("un type de véhicule inconnu lève 404 avant toute écriture")
		void typeInconnu() {
			when(typeVehiculeRepository.findById(1L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createVehicule(requete()))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Type de véhicule");
			verify(vehiculeRepository, never()).save(any());
		}

		@Test
		@DisplayName("une marque référencée par id est résolue")
		void marqueParId() {
			MarqueEntity marque = MarqueEntity.builder().id(4L).libelle("Toyota").build();
			VehiculeEntity entity = new VehiculeEntity();
			VehiculeRequest request = VehiculeRequest.builder()
					.immatriculation("AB-123-CD").typeId(1L).marqueId(4L).build();
			stubCreation(entity, request);
			when(marqueRepository.findById(4L)).thenReturn(Optional.of(marque));

			service.createVehicule(request);

			assertThat(entity.getMarque()).isSameAs(marque);
		}

		@Test
		@DisplayName("une marque donnée en texte libre est créée à la volée")
		void marqueTexteLibre() {
			MarqueEntity marque = MarqueEntity.builder().id(4L).libelle("Toyota").build();
			VehiculeEntity entity = new VehiculeEntity();
			VehiculeRequest request = VehiculeRequest.builder()
					.immatriculation("AB-123-CD").typeId(1L).marque("Toyota").build();
			stubCreation(entity, request);
			when(sharedService.getOrCreateMarque("Toyota")).thenReturn(marque);

			service.createVehicule(request);

			assertThat(entity.getMarque()).isSameAs(marque);
			verify(sharedService).getOrCreateMarque("Toyota");
		}

		@Test
		@DisplayName("sans marque, la référence est détachée")
		void sansMarque() {
			VehiculeEntity entity = VehiculeEntity.builder()
					.marque(MarqueEntity.builder().id(9L).build()).build();
			VehiculeRequest request = requete();
			stubCreation(entity, request);

			service.createVehicule(request);

			assertThat(entity.getMarque()).isNull();
		}

		@Test
		@DisplayName("les références optionnelles inconnues lèvent 404")
		void referenceOptionnelleInconnue() {
			VehiculeRequest request = VehiculeRequest.builder()
					.immatriculation("AB-123-CD").typeId(1L).energieId(99L).build();
			stubCreation(new VehiculeEntity(), request);
			when(typeCarburantRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createVehicule(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Énergie");
		}

		@Test
		@DisplayName("un identifiant de photo inconnu lève 404")
		void photoInconnue() {
			VehiculeRequest request = VehiculeRequest.builder()
					.immatriculation("AB-123-CD").typeId(1L).photoAvantId("media-404").build();
			stubCreation(new VehiculeEntity(), request);
			when(mediaRepository.findById("media-404")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createVehicule(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("media-404");
		}

		@Test
		@DisplayName("updateVehicule conserve le statut courant du véhicule")
		void updateConserveStatut() {
			VehiculeEntity entity = VehiculeEntity.builder().id(1L).statut(VehiculeStatusEnum.MISSION).build();
			VehiculeRequest request = requete();
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(typeVehiculeRepository.findById(1L))
					.thenReturn(Optional.of(TypeVehiculeEntity.builder().id(1L).build()));
			when(vehiculeRepository.save(entity)).thenReturn(entity);
			when(vehiculeMapper.toDto(entity)).thenReturn(Vehicule.builder().build());

			service.updateVehicule(1L, request);

			assertThat(entity.getStatut()).isEqualTo(VehiculeStatusEnum.MISSION);
			verify(vehiculeMapper).updateEntity(request, entity);
		}

		@Test
		@DisplayName("updateStatut applique le statut demandé")
		void updateStatut() {
			VehiculeEntity entity = VehiculeEntity.builder().id(1L).statut(VehiculeStatusEnum.DISPONIBLE).build();
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(vehiculeRepository.save(entity)).thenReturn(entity);
			when(vehiculeMapper.toDto(entity)).thenReturn(Vehicule.builder().build());

			service.updateStatut(1L, VehiculeStatusEnum.REFORME);

			assertThat(entity.getStatut()).isEqualTo(VehiculeStatusEnum.REFORME);
		}

		@Test
		@DisplayName("supprimer un véhicule inconnu lève 404")
		void delete_absent() {
			when(vehiculeRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteVehicule(99L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(vehiculeRepository, never()).deleteById(any());
		}
	}

	@Nested
	@DisplayName("Photos")
	class Photos {

		@Test
		@DisplayName("seules les photos fournies et non vides sont remplacées")
		void photosPartielles() {
			VehiculeEntity entity = VehiculeEntity.builder().id(1L).build();
			MediaEntity media = MediaEntity.builder().id("m1").build();
			var avant = new MockMultipartFile("f", "avant.png", "image/png", new byte[]{1});
			var arriereVide = new MockMultipartFile("f", "arriere.png", "image/png", new byte[0]);
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(mediaService.uploadMedia(avant)).thenReturn(Media.builder().id("m1").build());
			when(mediaService.getMediaEntity("m1")).thenReturn(media);
			when(vehiculeRepository.save(entity)).thenReturn(entity);
			when(vehiculeMapper.toDto(entity)).thenReturn(Vehicule.builder().build());

			service.updatePhotos(1L, avant, arriereVide, null, null);

			assertThat(entity.getPhotoAvant()).isSameAs(media);
			assertThat(entity.getPhotoArriere()).isNull();
			assertThat(entity.getPhotoCoteDroit()).isNull();
		}
	}

	@Nested
	@DisplayName("Documents administratifs")
	class Documents {

		@Test
		@DisplayName("ajouter un document téléverse le fichier et le rattache au véhicule")
		void ajout() {
			VehiculeEntity vehicule = VehiculeEntity.builder().id(1L).build();
			MediaEntity media = MediaEntity.builder().id("m1").build();
			var fichier = new MockMultipartFile("f", "carte.pdf", "application/pdf", new byte[]{1});
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(mediaService.uploadDocument(fichier)).thenReturn(Media.builder().id("m1").build());
			when(mediaService.getMediaEntity("m1")).thenReturn(media);
			when(documentVehiculeRepository.save(any(DocumentVehiculeEntity.class)))
					.thenAnswer(i -> i.getArgument(0));
			when(documentVehiculeMapper.toDto(any())).thenReturn(DocumentVehicule.builder().build());

			service.addDocument(1L, "Carte grise", fichier);

			ArgumentCaptor<DocumentVehiculeEntity> captor =
					ArgumentCaptor.forClass(DocumentVehiculeEntity.class);
			verify(documentVehiculeRepository).save(captor.capture());
			assertThat(captor.getValue().getLabel()).isEqualTo("Carte grise");
			assertThat(captor.getValue().getVehicule()).isSameAs(vehicule);
			assertThat(captor.getValue().getMedia()).isSameAs(media);
		}

		@Test
		@DisplayName("supprimer un document d'un autre véhicule est refusé")
		void suppressionCroisee() {
			DocumentVehiculeEntity document = DocumentVehiculeEntity.builder()
					.id(5L).vehicule(VehiculeEntity.builder().id(2L).build()).build();
			when(documentVehiculeRepository.findById(5L)).thenReturn(Optional.of(document));

			assertThatThrownBy(() -> service.deleteDocument(1L, 5L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("n'appartient pas au véhicule 1");
			verify(documentVehiculeRepository, never()).delete(any());
		}

		@Test
		@DisplayName("supprimer un document efface aussi son média")
		void suppression() {
			DocumentVehiculeEntity document = DocumentVehiculeEntity.builder()
					.id(5L).vehicule(VehiculeEntity.builder().id(1L).build())
					.media(MediaEntity.builder().id("m1").build()).build();
			when(documentVehiculeRepository.findById(5L)).thenReturn(Optional.of(document));

			service.deleteDocument(1L, 5L);

			verify(documentVehiculeRepository).delete(document);
			verify(mediaService).deleteMedia("m1");
		}

		@Test
		@DisplayName("lister les documents d'un véhicule inconnu lève 404")
		void listeVehiculeInconnu() {
			when(vehiculeRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.getDocuments(99L))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("Historique financier du véhicule")
	class Historique {

		private VehiculeEntity vehicule() {
			return VehiculeEntity.builder().id(1L).numChassis("VF123").immatriculation("AB-123-CD").build();
		}

		private MissionEntity mission(String code, LocalDateTime annulation) {
			return MissionEntity.builder()
					.id(1L).codeMission(code).dhmsAnnulation(annulation)
					.montantTotalHT(BigDecimal.valueOf(300_000)).build();
		}

		private FactureEntity facture(FactureStatusEnum statut, float tva) {
			return FactureEntity.builder().id(9L).numFacture("DA/01/79/1").statut(statut).tva(tva).build();
		}

		@Test
		@DisplayName("un chassis inconnu lève 404")
		void chassisInconnu() {
			when(vehiculeRepository.findByNumChassis("VF404")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getHistorique("VF404"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("VF404");
		}

		@Test
		@DisplayName("seules les factures PAYEE alimentent les gains, TVA appliquée au HT de la mission")
		void gainsSurFacturesPayees() {
			VehiculeEntity vehicule = vehicule();
			MissionEntity mission = mission("2026-001", null);
			FactureEntity facture = facture(FactureStatusEnum.PAYEE, 18f);
			LigneFactureEntity ligne = LigneFactureEntity.builder()
					.id(1L).extraRef("2026-001").montantHt(100_000L).facture(facture).build();

			when(vehiculeRepository.findByNumChassis("VF123")).thenReturn(Optional.of(vehicule));
			when(vehiculeMapper.toDto(vehicule)).thenReturn(Vehicule.builder().build());
			when(missionRepository.findByVehiculeIdOrderByDhmsDebutPreviDesc(1L)).thenReturn(List.of(mission));
			when(ligneFactureRepository.findByExtraRefInForMission(List.of("2026-001"))).thenReturn(List.of(ligne));
			when(depenseMissionRepository.findByMissionId(1L)).thenReturn(List.of());
			when(interventionRepository.findByVehiculeIdOrderByDhmsDebutDesc(1L)).thenReturn(List.of());

			VehiculeHistorique historique = service.getHistorique("VF123");

			assertThat(historique.getTotalGains()).isEqualTo(118_000L);
			assertThat(historique.getMissions()).hasSize(1);
			assertThat(historique.getMissions().get(0).getMontantFactureTtc()).isEqualTo(118_000L);
		}

		@Test
		@DisplayName("une facture non payée n'alimente pas les gains mais reste affichée")
		void factureNonPayee() {
			VehiculeEntity vehicule = vehicule();
			MissionEntity mission = mission("2026-001", null);
			FactureEntity facture = facture(FactureStatusEnum.PROFORMA, 0f);
			LigneFactureEntity ligne = LigneFactureEntity.builder()
					.id(1L).extraRef("2026-001").montantHt(100_000L).facture(facture).build();

			when(vehiculeRepository.findByNumChassis("VF123")).thenReturn(Optional.of(vehicule));
			when(vehiculeMapper.toDto(vehicule)).thenReturn(Vehicule.builder().build());
			when(missionRepository.findByVehiculeIdOrderByDhmsDebutPreviDesc(1L)).thenReturn(List.of(mission));
			when(ligneFactureRepository.findByExtraRefInForMission(any())).thenReturn(List.of(ligne));
			when(depenseMissionRepository.findByMissionId(1L)).thenReturn(List.of());
			when(interventionRepository.findByVehiculeIdOrderByDhmsDebutDesc(1L)).thenReturn(List.of());

			VehiculeHistorique historique = service.getHistorique("VF123");

			assertThat(historique.getTotalGains()).isZero();
			assertThat(historique.getMissions().get(0).getMontantFactureTtc()).isEqualTo(100_000L);
		}

		@Test
		@DisplayName("une mission annulée est affichée mais exclue des agrégats")
		void missionAnnuleeExclue() {
			VehiculeEntity vehicule = vehicule();
			MissionEntity mission = mission("2026-001", LocalDateTime.now());
			FactureEntity facture = facture(FactureStatusEnum.PAYEE, 0f);
			LigneFactureEntity ligne = LigneFactureEntity.builder()
					.id(1L).extraRef("2026-001").montantHt(100_000L).facture(facture).build();
			DepenseMissionEntity depense = DepenseMissionEntity.builder().id(1L).montant(20_000L).build();

			when(vehiculeRepository.findByNumChassis("VF123")).thenReturn(Optional.of(vehicule));
			when(vehiculeMapper.toDto(vehicule)).thenReturn(Vehicule.builder().build());
			when(missionRepository.findByVehiculeIdOrderByDhmsDebutPreviDesc(1L)).thenReturn(List.of(mission));
			when(ligneFactureRepository.findByExtraRefInForMission(any())).thenReturn(List.of(ligne));
			when(depenseMissionRepository.findByMissionId(1L)).thenReturn(List.of(depense));
			when(depenseMissionMapper.toDtoList(any())).thenReturn(List.of());
			when(interventionRepository.findByVehiculeIdOrderByDhmsDebutDesc(1L)).thenReturn(List.of());

			VehiculeHistorique historique = service.getHistorique("VF123");

			assertThat(historique.getTotalGains()).isZero();
			assertThat(historique.getTotalDepensesMissions()).isZero();
			assertThat(historique.getMissions()).hasSize(1);
			assertThat(historique.getMissions().get(0).isAnnulee()).isTrue();
			assertThat(historique.getMissions().get(0).getTotalDepenses()).isEqualTo(20_000L);
		}

		@Test
		@DisplayName("le solde agrège dépenses de missions et coûts d'interventions")
		void solde() {
			VehiculeEntity vehicule = vehicule();
			MissionEntity mission = mission("2026-001", null);
			DepenseMissionEntity depense = DepenseMissionEntity.builder().id(1L).montant(20_000L).build();
			InterventionEntity intervention = InterventionEntity.builder().id(1L).cout(50_000L).build();

			when(vehiculeRepository.findByNumChassis("VF123")).thenReturn(Optional.of(vehicule));
			when(vehiculeMapper.toDto(vehicule)).thenReturn(Vehicule.builder().build());
			when(missionRepository.findByVehiculeIdOrderByDhmsDebutPreviDesc(1L)).thenReturn(List.of(mission));
			when(ligneFactureRepository.findByExtraRefInForMission(any())).thenReturn(List.of());
			when(depenseMissionRepository.findByMissionId(1L)).thenReturn(List.of(depense));
			when(depenseMissionMapper.toDtoList(any())).thenReturn(List.of());
			when(interventionRepository.findByVehiculeIdOrderByDhmsDebutDesc(1L)).thenReturn(List.of(intervention));

			VehiculeHistorique historique = service.getHistorique("VF123");

			assertThat(historique.getTotalDepensesMissions()).isEqualTo(20_000L);
			assertThat(historique.getTotalDepensesInterventions()).isEqualTo(50_000L);
			assertThat(historique.getTotalDepenses()).isEqualTo(70_000L);
			assertThat(historique.getSolde()).isEqualTo(-70_000L);
		}

		@Test
		@DisplayName("un véhicule sans mission ni intervention donne un historique à zéro")
		void historiqueVide() {
			VehiculeEntity vehicule = vehicule();
			when(vehiculeRepository.findByNumChassis("VF123")).thenReturn(Optional.of(vehicule));
			when(vehiculeMapper.toDto(vehicule)).thenReturn(Vehicule.builder().build());
			when(missionRepository.findByVehiculeIdOrderByDhmsDebutPreviDesc(1L)).thenReturn(List.of());
			when(interventionRepository.findByVehiculeIdOrderByDhmsDebutDesc(1L)).thenReturn(List.of());

			VehiculeHistorique historique = service.getHistorique("VF123");

			assertThat(historique.getTotalGains()).isZero();
			assertThat(historique.getTotalDepenses()).isZero();
			assertThat(historique.getSolde()).isZero();
			assertThat(historique.getMissions()).isEmpty();
			assertThat(historique.getInterventions()).isEmpty();
			verify(ligneFactureRepository, never()).findByExtraRefInForMission(any());
		}
	}

	@Nested
	@DisplayName("Consultation")
	class Consultation {

		@Test
		@DisplayName("getVehiculeByImmatriculation lève 404 sur une immatriculation inconnue")
		void parImmatriculation() {
			when(vehiculeRepository.findByImmatriculation("ZZ-999-ZZ")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getVehiculeByImmatriculation("ZZ-999-ZZ"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("ZZ-999-ZZ");
		}

		@Test
		@DisplayName("un mot-clé blanc est transmis comme filtre nul à la requête")
		void motCleBlanc() {
			var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
			when(vehiculeRepository.searchWithFilters(null, null, null, null, pageable))
					.thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));

			service.getAllVehicules("   ", null, null, null, pageable);

			verify(vehiculeRepository).searchWithFilters(null, null, null, null, pageable);
		}
	}
}
