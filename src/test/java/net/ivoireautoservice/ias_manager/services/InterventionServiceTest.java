package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.Intervention;
import net.ivoireautoservice.ias_manager.dto.request.InterventionRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.entity.InterventionEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.TypeInterventionEntity;
import net.ivoireautoservice.ias_manager.entity.VehiculeEntity;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.InterventionMapper;
import net.ivoireautoservice.ias_manager.repository.InterventionRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import net.ivoireautoservice.ias_manager.repository.TypeInterventionRepository;
import net.ivoireautoservice.ias_manager.repository.VehiculeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterventionService — cycle de vie des interventions et impact véhicule")
class InterventionServiceTest {

	@Mock
	private InterventionRepository interventionRepository;

	@Mock
	private VehiculeRepository vehiculeRepository;

	@Mock
	private TypeInterventionRepository typeInterventionRepository;

	@Mock
	private PartenaireRepository partenaireRepository;

	@Mock
	private InterventionMapper interventionMapper;

	@Mock
	private CompteService compteService;

	@InjectMocks
	private InterventionService service;

	private static VehiculeEntity vehicule(VehiculeStatusEnum statut) {
		return VehiculeEntity.builder().id(1L).immatriculation("AB-123-CD").statut(statut).build();
	}

	private static InterventionEntity intervention(InterventionStatut statut, VehiculeEntity vehicule) {
		return InterventionEntity.builder().id(1L).statut(statut).vehicule(vehicule).build();
	}

	@Nested
	@DisplayName("Création")
	class Creation {

		private InterventionRequest request(LocalDate debut) {
			return InterventionRequest.builder()
					.vehiculeId(1L).typeInterventionId(2L).dhmsDebut(debut).build();
		}

		private InterventionEntity prepare(LocalDate debut, VehiculeEntity vehicule) {
			InterventionEntity entity = InterventionEntity.builder()
					.statut(InterventionStatut.CREEE).dhmsDebut(debut).build();
			InterventionRequest request = request(debut);
			when(interventionMapper.toEntity(request)).thenReturn(entity);
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
			when(typeInterventionRepository.findById(2L))
					.thenReturn(Optional.of(TypeInterventionEntity.builder().id(2L).build()));
			when(interventionRepository.save(entity)).thenReturn(entity);
			when(interventionMapper.toDto(entity)).thenReturn(new Intervention());
			service.createIntervention(request);
			return entity;
		}

		@Test
		@DisplayName("une intervention démarrant aujourd'hui passe EN_COURS et envoie le véhicule au garage")
		void demarrageImmediat() {
			VehiculeEntity vehicule = vehicule(VehiculeStatusEnum.DISPONIBLE);

			InterventionEntity entity = prepare(LocalDate.now(), vehicule);

			assertThat(entity.getStatut()).isEqualTo(InterventionStatut.EN_COURS);
			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.GARAGE);
			verify(vehiculeRepository).save(vehicule);
		}

		@Test
		@DisplayName("une intervention prévue demain démarre également immédiatement")
		void demarrageDemain() {
			VehiculeEntity vehicule = vehicule(VehiculeStatusEnum.DISPONIBLE);

			InterventionEntity entity = prepare(LocalDate.now().plusDays(1), vehicule);

			assertThat(entity.getStatut()).isEqualTo(InterventionStatut.EN_COURS);
			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.GARAGE);
		}

		@Test
		@DisplayName("une intervention planifiée plus tard reste CREEE et laisse le véhicule disponible")
		void planifieePlusTard() {
			VehiculeEntity vehicule = vehicule(VehiculeStatusEnum.DISPONIBLE);

			InterventionEntity entity = prepare(LocalDate.now().plusDays(5), vehicule);

			assertThat(entity.getStatut()).isEqualTo(InterventionStatut.CREEE);
			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.DISPONIBLE);
			verify(vehiculeRepository, never()).save(any());
		}

		@Test
		@DisplayName("un véhicule en mission n'est pas basculé au garage")
		void vehiculeEnMissionPreserve() {
			VehiculeEntity vehicule = vehicule(VehiculeStatusEnum.MISSION);

			prepare(LocalDate.now(), vehicule);

			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.MISSION);
			verify(vehiculeRepository, never()).save(any());
		}

		@Test
		@DisplayName("un véhicule inconnu lève 404")
		void vehiculeInconnu() {
			InterventionRequest request = request(LocalDate.now());
			when(interventionMapper.toEntity(request)).thenReturn(new InterventionEntity());
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createIntervention(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Véhicule avec l'id 1");
		}

		@Test
		@DisplayName("un type d'intervention inconnu lève 404")
		void typeInconnu() {
			InterventionRequest request = request(LocalDate.now());
			when(interventionMapper.toEntity(request)).thenReturn(new InterventionEntity());
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule(VehiculeStatusEnum.DISPONIBLE)));
			when(typeInterventionRepository.findById(2L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createIntervention(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Type d'intervention");
		}

		@Test
		@DisplayName("le garage est résolu quand il est fourni, détaché sinon")
		void garage() {
			PartenaireEntity garage = PartenaireEntity.builder().id(4L).build();
			InterventionEntity entity = InterventionEntity.builder()
					.garage(PartenaireEntity.builder().id(9L).build()).build();
			InterventionRequest request = InterventionRequest.builder()
					.vehiculeId(1L).typeInterventionId(2L).garageId(4L).build();
			when(interventionMapper.toEntity(request)).thenReturn(entity);
			when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule(VehiculeStatusEnum.DISPONIBLE)));
			when(typeInterventionRepository.findById(2L))
					.thenReturn(Optional.of(TypeInterventionEntity.builder().id(2L).build()));
			when(partenaireRepository.findById(4L)).thenReturn(Optional.of(garage));
			when(interventionRepository.save(entity)).thenReturn(entity);
			when(interventionMapper.toDto(entity)).thenReturn(new Intervention());

			service.createIntervention(request);

			assertThat(entity.getGarage()).isSameAs(garage);
		}
	}

	@Nested
	@DisplayName("Démarrage")
	class Demarrage {

		@Test
		@DisplayName("une intervention CREEE passe EN_COURS et envoie le véhicule au garage")
		void demarrage() {
			VehiculeEntity vehicule = vehicule(VehiculeStatusEnum.DISPONIBLE);
			InterventionEntity entity = intervention(InterventionStatut.CREEE, vehicule);
			when(interventionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(interventionRepository.save(entity)).thenReturn(entity);
			when(interventionMapper.toDto(entity)).thenReturn(new Intervention());

			service.demarrerIntervention(1L);

			assertThat(entity.getStatut()).isEqualTo(InterventionStatut.EN_COURS);
			assertThat(entity.getDhmsDebut()).isEqualTo(LocalDate.now());
			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.GARAGE);
		}

		@Test
		@DisplayName("une intervention déjà EN_COURS ne peut pas être redémarrée")
		void dejaDemarree() {
			InterventionEntity entity = intervention(InterventionStatut.EN_COURS, vehicule(VehiculeStatusEnum.GARAGE));
			when(interventionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.demarrerIntervention(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("statut CREEE");
		}

		@Test
		@DisplayName("un véhicule en mission conserve son statut au démarrage")
		void vehiculeEnMission() {
			VehiculeEntity vehicule = vehicule(VehiculeStatusEnum.MISSION);
			InterventionEntity entity = intervention(InterventionStatut.CREEE, vehicule);
			when(interventionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(interventionRepository.save(entity)).thenReturn(entity);
			when(interventionMapper.toDto(entity)).thenReturn(new Intervention());

			service.demarrerIntervention(1L);

			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.MISSION);
			verify(vehiculeRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("Clôture")
	class Cloture {

		@Test
		@DisplayName("clôturer rend le véhicule disponible quand la réparation est terminée")
		void clotureVehiculeDisponible() {
			VehiculeEntity vehicule = vehicule(VehiculeStatusEnum.GARAGE);
			InterventionEntity entity = intervention(InterventionStatut.EN_COURS, vehicule);
			when(interventionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(interventionRepository.save(entity)).thenReturn(entity);
			when(interventionMapper.toDto(entity)).thenReturn(new Intervention());

			service.cloturerIntervention(1L, true);

			assertThat(entity.getStatut()).isEqualTo(InterventionStatut.CLOTUREE);
			assertThat(entity.getDhmsFin()).isEqualTo(LocalDate.now());
			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.DISPONIBLE);
		}

		@Test
		@DisplayName("clôturer laisse le véhicule au garage si des travaux restent à faire")
		void clotureVehiculeAuGarage() {
			VehiculeEntity vehicule = vehicule(VehiculeStatusEnum.GARAGE);
			InterventionEntity entity = intervention(InterventionStatut.EN_COURS, vehicule);
			when(interventionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(interventionRepository.save(entity)).thenReturn(entity);
			when(interventionMapper.toDto(entity)).thenReturn(new Intervention());

			service.cloturerIntervention(1L, false);

			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.GARAGE);
		}

		@Test
		@DisplayName("seule une intervention EN_COURS peut être clôturée")
		void statutInvalide() {
			InterventionEntity entity = intervention(InterventionStatut.CREEE, vehicule(VehiculeStatusEnum.DISPONIBLE));
			when(interventionRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.cloturerIntervention(1L, true))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("statut EN_COURS");
		}

		@Test
		@DisplayName("la clôture ne touche jamais à la trésorerie, même avec un coût connu")
		void clotureSansMouvementDeCompte() {
			InterventionEntity entity = InterventionEntity.builder()
					.id(1L).statut(InterventionStatut.EN_COURS)
					.vehicule(vehicule(VehiculeStatusEnum.GARAGE))
					.cout(75_000L).objet("Changement plaquettes")
					.build();
			when(interventionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(interventionRepository.save(entity)).thenReturn(entity);
			when(interventionMapper.toDto(entity)).thenReturn(new Intervention());

			service.cloturerIntervention(1L, true);

			// Décaisser ici puis à nouveau au règlement débiterait deux fois le même coût :
			// seul payerIntervention sort l'argent de la caisse.
			verifyNoInteractions(compteService);
			assertThat(entity.getDhmsPaiement()).isNull();
		}

		@Test
		@DisplayName("un véhicule en mission n'est pas restitué par la clôture")
		void vehiculeEnMission() {
			VehiculeEntity vehicule = vehicule(VehiculeStatusEnum.MISSION);
			InterventionEntity entity = intervention(InterventionStatut.EN_COURS, vehicule);
			when(interventionRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(interventionRepository.save(entity)).thenReturn(entity);
			when(interventionMapper.toDto(entity)).thenReturn(new Intervention());

			service.cloturerIntervention(1L, true);

			assertThat(vehicule.getStatut()).isEqualTo(VehiculeStatusEnum.MISSION);
			verify(vehiculeRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("Consultation")
	class Consultation {

		@Test
		@DisplayName("un mot-clé déclenche la recherche, détrimé")
		void recherche() {
			var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
			when(interventionRepository.searchByKeyword("frein", pageable))
					.thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(), pageable, 0));

			service.getAllInterventions(" frein ", pageable);

			verify(interventionRepository).searchByKeyword("frein", pageable);
		}

		@Test
		@DisplayName("lister les interventions d'un véhicule inconnu lève 404")
		void parVehiculeInconnu() {
			var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
			when(vehiculeRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.getInterventionsByVehicule(99L, pageable))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("supprimer une intervention inconnue lève 404")
		void delete_absente() {
			when(interventionRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.deleteIntervention(99L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(interventionRepository, never()).deleteById(any());
		}
	}
}
