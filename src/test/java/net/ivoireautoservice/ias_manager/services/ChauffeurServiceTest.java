package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.Chauffeur;
import net.ivoireautoservice.ias_manager.dto.request.ChauffeurRequest;
import net.ivoireautoservice.ias_manager.entity.ChauffeurEntity;
import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import net.ivoireautoservice.ias_manager.enums.StatutChauffeurEnum;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.ChauffeurMapper;
import net.ivoireautoservice.ias_manager.repository.ChauffeurRepository;
import net.ivoireautoservice.ias_manager.repository.EmployeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChauffeurService — registre des chauffeurs")
class ChauffeurServiceTest {

	@Mock
	private ChauffeurRepository chauffeurRepository;

	@Mock
	private EmployeRepository employeRepository;

	@Mock
	private ChauffeurMapper chauffeurMapper;

	@InjectMocks
	private ChauffeurService service;

	private final Pageable pageable = PageRequest.of(0, 10);

	private Page<ChauffeurEntity> unePage() {
		return new PageImpl<>(List.of(ChauffeurEntity.builder().id(1L).build()), pageable, 1);
	}

	@Nested
	@DisplayName("Recherche paginée : sélection de la requête selon les filtres")
	class Recherche {

		@Test
		@DisplayName("mot-clé et statut → recherche combinée, mot-clé détrimé")
		void motCleEtStatut() {
			when(chauffeurRepository.searchByKeywordAndStatut("kouassi", StatutChauffeurEnum.DISPONIBLE, pageable))
					.thenReturn(unePage());

			service.getAllChauffeurs("  kouassi  ", StatutChauffeurEnum.DISPONIBLE, pageable);

			verify(chauffeurRepository).searchByKeywordAndStatut("kouassi", StatutChauffeurEnum.DISPONIBLE, pageable);
		}

		@Test
		@DisplayName("mot-clé seul → recherche par mot-clé")
		void motCleSeul() {
			when(chauffeurRepository.searchByKeyword("kouassi", pageable)).thenReturn(unePage());

			service.getAllChauffeurs("kouassi", null, pageable);

			verify(chauffeurRepository).searchByKeyword("kouassi", pageable);
		}

		@Test
		@DisplayName("statut seul → filtrage par statut")
		void statutSeul() {
			when(chauffeurRepository.findByStatut(StatutChauffeurEnum.MISSION, pageable)).thenReturn(unePage());

			service.getAllChauffeurs(null, StatutChauffeurEnum.MISSION, pageable);

			verify(chauffeurRepository).findByStatut(StatutChauffeurEnum.MISSION, pageable);
		}

		@Test
		@DisplayName("aucun filtre → findAll")
		void aucunFiltre() {
			when(chauffeurRepository.findAll(pageable)).thenReturn(unePage());

			service.getAllChauffeurs(null, null, pageable);

			verify(chauffeurRepository).findAll(pageable);
		}

		@Test
		@DisplayName("un mot-clé composé d'espaces est ignoré")
		void motCleBlanc() {
			when(chauffeurRepository.findAll(pageable)).thenReturn(unePage());

			service.getAllChauffeurs("   ", null, pageable);

			verify(chauffeurRepository).findAll(pageable);
			verify(chauffeurRepository, never()).searchByKeyword(any(), any());
		}
	}

	@Nested
	@DisplayName("Cycle de vie")
	class CycleDeVie {

		@Test
		@DisplayName("un chauffeur est créé DISPONIBLE, employé résolu")
		void create() {
			EmployeEntity employe = EmployeEntity.builder().id(7L).nom("Kouassi").build();
			ChauffeurRequest request = ChauffeurRequest.builder().employeId(7L).numeroPermis("P-1").build();
			ChauffeurEntity entity = new ChauffeurEntity();
			when(chauffeurMapper.toEntity(request)).thenReturn(entity);
			when(employeRepository.findById(7L)).thenReturn(Optional.of(employe));
			when(chauffeurRepository.save(entity)).thenReturn(entity);
			when(chauffeurMapper.toDto(entity)).thenReturn(Chauffeur.builder().build());

			service.createChauffeur(request);

			assertThat(entity.getStatut()).isEqualTo(StatutChauffeurEnum.DISPONIBLE);
			assertThat(entity.getEmploye()).isSameAs(employe);
		}

		@Test
		@DisplayName("la création échoue si l'employé référencé n'existe pas")
		void create_employeInconnu() {
			ChauffeurRequest request = ChauffeurRequest.builder().employeId(99L).build();
			when(chauffeurMapper.toEntity(request)).thenReturn(new ChauffeurEntity());
			when(employeRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createChauffeur(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Employé avec l'id 99");
		}

		@Test
		@DisplayName("sans employeId, le chauffeur est détaché de tout employé")
		void create_sansEmploye() {
			ChauffeurRequest request = ChauffeurRequest.builder().numeroPermis("P-1").build();
			ChauffeurEntity entity = ChauffeurEntity.builder()
					.employe(EmployeEntity.builder().id(1L).build()).build();
			when(chauffeurMapper.toEntity(request)).thenReturn(entity);
			when(chauffeurRepository.save(entity)).thenReturn(entity);
			when(chauffeurMapper.toDto(entity)).thenReturn(Chauffeur.builder().build());

			service.createChauffeur(request);

			assertThat(entity.getEmploye()).isNull();
		}

		@Test
		@DisplayName("changerStatut applique le nouveau statut")
		void changerStatut() {
			ChauffeurEntity entity = ChauffeurEntity.builder().id(1L)
					.statut(StatutChauffeurEnum.DISPONIBLE).build();
			when(chauffeurRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(chauffeurRepository.save(entity)).thenReturn(entity);
			when(chauffeurMapper.toDto(entity)).thenReturn(Chauffeur.builder().build());

			service.changerStatut(1L, StatutChauffeurEnum.MISSION);

			assertThat(entity.getStatut()).isEqualTo(StatutChauffeurEnum.MISSION);
		}

		@Test
		@DisplayName("updateChauffeur applique la requête sur l'entité existante")
		void update() {
			ChauffeurEntity entity = ChauffeurEntity.builder().id(1L).build();
			ChauffeurRequest request = ChauffeurRequest.builder().numeroPermis("P-2").build();
			when(chauffeurRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(chauffeurRepository.save(entity)).thenReturn(entity);
			when(chauffeurMapper.toDto(entity)).thenReturn(Chauffeur.builder().build());

			service.updateChauffeur(1L, request);

			verify(chauffeurMapper).updateEntity(request, entity);
		}

		@Test
		@DisplayName("la suppression d'un chauffeur inconnu lève 404")
		void delete_absent() {
			when(chauffeurRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteChauffeur(99L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(chauffeurRepository, never()).deleteById(any());
		}
	}

	@Nested
	@DisplayName("Accès unitaires")
	class AccesUnitaires {

		@Test
		@DisplayName("getChauffeurById lève 404 sur un id inconnu")
		void parId() {
			when(chauffeurRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getChauffeurById(99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Chauffeur avec l'id 99");
		}

		@Test
		@DisplayName("getChauffeurByEmployeId lève 404 si l'employé n'est pas chauffeur")
		void parEmploye() {
			when(chauffeurRepository.findByEmployeId(7L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getChauffeurByEmployeId(7L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("employé id 7");
		}

		@Test
		@DisplayName("getChauffeurByNumeroPermis lève 404 sur un permis inconnu")
		void parPermis() {
			when(chauffeurRepository.findByNumeroPermis("P-404")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getChauffeurByNumeroPermis("P-404"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("P-404");
		}
	}
}
