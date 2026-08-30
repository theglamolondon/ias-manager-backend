package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.Employe;
import net.ivoireautoservice.ias_manager.dto.request.EmployeRequest;
import net.ivoireautoservice.ias_manager.entity.ChauffeurEntity;
import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import net.ivoireautoservice.ias_manager.entity.ServiceEntity;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.EmployeMapper;
import net.ivoireautoservice.ias_manager.repository.ChauffeurRepository;
import net.ivoireautoservice.ias_manager.repository.EmployeRepository;
import net.ivoireautoservice.ias_manager.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeService — employés et synchronisation du profil chauffeur")
class EmployeServiceTest {

	@Mock
	private EmployeRepository employeRepository;

	@Mock
	private ServiceRepository serviceRepository;

	@Mock
	private ChauffeurRepository chauffeurRepository;

	@Mock
	private EmployeMapper employeMapper;

	@InjectMocks
	private EmployeService service;

	private EmployeEntity employe;

	@BeforeEach
	void setUp() {
		employe = EmployeEntity.builder().id(1L).matricule("E-001").nom("Kouassi").build();
	}

	private void stubMapperEtSauvegarde(EmployeRequest request) {
		when(employeMapper.toEntity(request)).thenReturn(employe);
		when(employeRepository.save(employe)).thenReturn(employe);
		when(employeMapper.toDto(employe)).thenReturn(new Employe());
	}

	@Nested
	@DisplayName("Profil chauffeur")
	class ProfilChauffeur {

		@Test
		@DisplayName("isChauffeur=true crée le profil chauffeur avec permis et type")
		void creeChauffeur() {
			EmployeRequest request = EmployeRequest.builder()
					.matricule("E-001").isChauffeur(true)
					.numeroPermis("P-123").typePermis("B")
					.expDatePermis(LocalDate.of(2030, 1, 1))
					.build();
			stubMapperEtSauvegarde(request);
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.empty());
			when(chauffeurRepository.save(any(ChauffeurEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.createEmploye(request);

			ArgumentCaptor<ChauffeurEntity> captor = ArgumentCaptor.forClass(ChauffeurEntity.class);
			verify(chauffeurRepository).save(captor.capture());
			assertThat(captor.getValue().getEmploye()).isSameAs(employe);
			assertThat(captor.getValue().getNumeroPermis()).isEqualTo("P-123");
			assertThat(captor.getValue().getTypePermis()).isEqualTo("B");
			assertThat(captor.getValue().getExpDatePermis()).isEqualTo(LocalDate.of(2030, 1, 1));
		}

		@Test
		@DisplayName("isChauffeur=true sans numéro de permis est refusé")
		void permisObligatoire() {
			EmployeRequest request = EmployeRequest.builder()
					.matricule("E-001").isChauffeur(true).typePermis("B").build();
			when(employeMapper.toEntity(request)).thenReturn(employe);
			when(employeRepository.save(employe)).thenReturn(employe);
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createEmploye(request))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("numéro de permis");
		}

		@Test
		@DisplayName("isChauffeur=true sans type de permis est refusé")
		void typePermisObligatoire() {
			EmployeRequest request = EmployeRequest.builder()
					.matricule("E-001").isChauffeur(true).numeroPermis("P-123").typePermis("  ").build();
			when(employeMapper.toEntity(request)).thenReturn(employe);
			when(employeRepository.save(employe)).thenReturn(employe);
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createEmploye(request))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("type de permis");
		}

		@Test
		@DisplayName("repasser isChauffeur à false supprime le profil chauffeur existant")
		void retireChauffeur() {
			ChauffeurEntity existant = ChauffeurEntity.builder().id(5L).employe(employe).build();
			EmployeRequest request = EmployeRequest.builder().matricule("E-001").isChauffeur(false).build();
			when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
			when(employeRepository.save(employe)).thenReturn(employe);
			when(employeMapper.toDto(employe)).thenReturn(new Employe());
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.of(existant));

			service.updateEmploye(1L, request);

			verify(chauffeurRepository).delete(existant);
			verify(chauffeurRepository, never()).save(any());
		}

		@Test
		@DisplayName("un profil chauffeur existant est mis à jour, pas dupliqué")
		void metAJourChauffeurExistant() {
			ChauffeurEntity existant = ChauffeurEntity.builder()
					.id(5L).employe(employe).numeroPermis("ANCIEN").typePermis("B").build();
			EmployeRequest request = EmployeRequest.builder()
					.matricule("E-001").isChauffeur(true).numeroPermis("NOUVEAU").typePermis("C").build();
			when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
			when(employeRepository.save(employe)).thenReturn(employe);
			when(employeMapper.toDto(employe)).thenReturn(new Employe());
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.of(existant));
			when(chauffeurRepository.save(existant)).thenReturn(existant);

			service.updateEmploye(1L, request);

			assertThat(existant.getNumeroPermis()).isEqualTo("NOUVEAU");
			assertThat(existant.getTypePermis()).isEqualTo("C");
			verify(chauffeurRepository).save(existant);
		}

		@Test
		@DisplayName("le DTO retourné porte les informations du permis quand l'employé est chauffeur")
		void dtoEnrichi() {
			ChauffeurEntity chauffeur = ChauffeurEntity.builder()
					.id(5L).employe(employe).numeroPermis("P-1").typePermis("B")
					.expDatePermis(LocalDate.of(2031, 5, 4)).build();
			when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.of(chauffeur));
			when(employeMapper.toDto(employe)).thenReturn(new Employe());

			Employe dto = service.getEmployeById(1L);

			assertThat(dto.getIsChauffeur()).isTrue();
			assertThat(dto.getChauffeurId()).isEqualTo(5L);
			assertThat(dto.getNumeroPermis()).isEqualTo("P-1");
			assertThat(dto.getTypePermis()).isEqualTo("B");
			assertThat(dto.getExpDatePermis()).isEqualTo(LocalDate.of(2031, 5, 4));
		}

		@Test
		@DisplayName("un employé non chauffeur est marqué isChauffeur=false")
		void dtoNonChauffeur() {
			when(employeRepository.findById(1L)).thenReturn(Optional.of(employe));
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.empty());
			when(employeMapper.toDto(employe)).thenReturn(new Employe());

			assertThat(service.getEmployeById(1L).getIsChauffeur()).isFalse();
		}
	}

	@Nested
	@DisplayName("Service d'affectation")
	class ServiceAffectation {

		@Test
		@DisplayName("le service est résolu quand serviceId est fourni")
		void serviceResolu() {
			ServiceEntity serviceEntity = ServiceEntity.builder().id(3L).build();
			EmployeRequest request = EmployeRequest.builder().matricule("E-001").serviceId(3L).build();
			stubMapperEtSauvegarde(request);
			when(serviceRepository.findById(3L)).thenReturn(Optional.of(serviceEntity));
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.empty());

			service.createEmploye(request);

			assertThat(employe.getService()).isSameAs(serviceEntity);
		}

		@Test
		@DisplayName("un serviceId inconnu lève 404")
		void serviceInconnu() {
			EmployeRequest request = EmployeRequest.builder().matricule("E-001").serviceId(99L).build();
			when(employeMapper.toEntity(request)).thenReturn(employe);
			when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createEmploye(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Service avec l'id 99");
		}

		@Test
		@DisplayName("sans serviceId, l'employé est détaché de son service")
		void serviceDetache() {
			employe.setService(ServiceEntity.builder().id(3L).build());
			EmployeRequest request = EmployeRequest.builder().matricule("E-001").build();
			stubMapperEtSauvegarde(request);
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.empty());

			service.createEmploye(request);

			assertThat(employe.getService()).isNull();
		}
	}

	@Nested
	@DisplayName("Consultation et suppression")
	class ConsultationSuppression {

		@Test
		@DisplayName("la liste complète associe chaque employé à son chauffeur éventuel")
		void listeAvecChauffeurs() {
			EmployeEntity second = EmployeEntity.builder().id(2L).build();
			ChauffeurEntity chauffeur = ChauffeurEntity.builder().id(9L).employe(employe).numeroPermis("P-9").build();
			when(employeRepository.findAll()).thenReturn(List.of(employe, second));
			when(chauffeurRepository.findAll()).thenReturn(List.of(chauffeur));
			when(employeMapper.toDto(any(EmployeEntity.class))).thenAnswer(i -> new Employe());

			List<Employe> resultat = service.getAllEmployes();

			assertThat(resultat).hasSize(2);
			assertThat(resultat.get(0).getIsChauffeur()).isTrue();
			assertThat(resultat.get(1).getIsChauffeur()).isFalse();
		}

		@Test
		@DisplayName("supprimer un employé supprime aussi son profil chauffeur")
		void deleteAvecChauffeur() {
			ChauffeurEntity chauffeur = ChauffeurEntity.builder().id(5L).build();
			when(employeRepository.existsById(1L)).thenReturn(true);
			when(chauffeurRepository.findByEmployeId(1L)).thenReturn(Optional.of(chauffeur));

			service.deleteEmploye(1L);

			verify(chauffeurRepository).delete(chauffeur);
			verify(employeRepository).deleteById(1L);
		}

		@Test
		@DisplayName("supprimer un employé inconnu lève 404")
		void delete_absent() {
			when(employeRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteEmploye(99L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(employeRepository, never()).deleteById(any());
		}

		@Test
		@DisplayName("getEmployeByMatricule lève 404 sur un matricule inconnu")
		void parMatricule_absent() {
			when(employeRepository.findByMatricule("E-404")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getEmployeByMatricule("E-404"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("E-404");
		}
	}
}
