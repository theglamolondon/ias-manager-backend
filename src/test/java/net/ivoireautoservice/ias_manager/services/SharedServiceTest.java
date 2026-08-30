package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.*;
import net.ivoireautoservice.ias_manager.dto.request.*;
import net.ivoireautoservice.ias_manager.entity.*;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SharedService — données de référence")
class SharedServiceTest {

	@Mock private CategoryRepository categoryRepository;
	@Mock private TypeVehiculeRepository typeVehiculeRepository;
	@Mock private MarqueRepository marqueRepository;
	@Mock private ServiceRepository serviceRepository;
	@Mock private TypeInterventionRepository typeInterventionRepository;
	@Mock private FamilleProduitRepository familleProduitRepository;
	@Mock private TypeDepenseRepository typeDepenseRepository;
	@Mock private TypeCarburantRepository typeCarburantRepository;
	@Mock private TypeAssuranceRepository typeAssuranceRepository;
	@Mock private AssuranceRepository assuranceRepository;
	@Mock private MediaService mediaService;
	@Mock private CategorieMapper categorieMapper;
	@Mock private TypeVehiculeMapper typeVehiculeMapper;
	@Mock private MarqueMapper marqueMapper;
	@Mock private ServiceMapper serviceMapper;
	@Mock private TypeInterventionMapper typeInterventionMapper;
	@Mock private FamilleProduitMapper familleProduitMapper;
	@Mock private TypeDepenseMapper typeDepenseMapper;
	@Mock private TypeCarburantMapper typeCarburantMapper;
	@Mock private TypeAssuranceMapper typeAssuranceMapper;
	@Mock private AssuranceMapper assuranceMapper;

	@InjectMocks
	private SharedService service;

	@Nested
	@DisplayName("Marques : création implicite")
	class Marques {

		@Test
		@DisplayName("une marque existante est réutilisée sans création")
		void marqueExistante() {
			MarqueEntity existante = MarqueEntity.builder().id(1L).libelle("Toyota").build();
			when(marqueRepository.findByLibelleIgnoreCase("toyota")).thenReturn(Optional.of(existante));

			assertThat(service.getOrCreateMarque("toyota")).isSameAs(existante);
			verify(marqueRepository, never()).save(any());
		}

		@Test
		@DisplayName("une marque inconnue est créée à la volée avec le libellé fourni")
		void marqueCreee() {
			when(marqueRepository.findByLibelleIgnoreCase("Hyundai")).thenReturn(Optional.empty());
			when(marqueRepository.save(any(MarqueEntity.class))).thenAnswer(i -> i.getArgument(0));

			MarqueEntity resultat = service.getOrCreateMarque("Hyundai");

			assertThat(resultat.getLibelle()).isEqualTo("Hyundai");
			verify(marqueRepository).save(any(MarqueEntity.class));
		}

		@Test
		@DisplayName("un id explicite fourni à la création est conservé (import de référentiel)")
		void idExplicite() {
			MarqueRequest request = MarqueRequest.builder().id(42L).libelle("Toyota").build();
			MarqueEntity entity = new MarqueEntity();
			when(marqueMapper.toEntity(request)).thenReturn(entity);
			when(marqueRepository.save(entity)).thenReturn(entity);
			when(marqueMapper.toDto(entity)).thenReturn(Marque.builder().build());

			service.createMarque(request);

			assertThat(entity.getId()).isEqualTo(42L);
		}
	}

	@Nested
	@DisplayName("Types de véhicule")
	class TypesVehicule {

		@Test
		@DisplayName("la catégorie est résolue à la création")
		void categorieResolue() {
			CategorieEntity categorie = CategorieEntity.builder().id(3L).build();
			TypeVehiculeRequest request = TypeVehiculeRequest.builder().categorieId(3L).build();
			TypeVehiculeEntity entity = new TypeVehiculeEntity();
			when(categoryRepository.findById(3L)).thenReturn(Optional.of(categorie));
			when(typeVehiculeMapper.toEntity(request)).thenReturn(entity);
			when(typeVehiculeRepository.save(entity)).thenReturn(entity);
			when(typeVehiculeMapper.toDto(entity)).thenReturn(TypeVehicule.builder().build());

			service.createTypeVehicule(request);

			assertThat(entity.getCategorie()).isSameAs(categorie);
		}

		@Test
		@DisplayName("une catégorie inconnue lève 404")
		void categorieInconnue() {
			when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createTypeVehicule(
					TypeVehiculeRequest.builder().categorieId(99L).build()))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Catégorie avec l'id 99");
		}

		@Test
		@DisplayName("getTypeVehiculeById lève 404 sur un id inconnu")
		void typeInconnu() {
			when(typeVehiculeRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getTypeVehiculeById(99L))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("supprimer un type inconnu lève 404")
		void suppressionInconnue() {
			when(typeVehiculeRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteTypeVehicule(99L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(typeVehiculeRepository, never()).deleteById(any());
		}
	}

	@Nested
	@DisplayName("Assurances et logos")
	class Assurances {

		@Test
		@DisplayName("un logo fourni est téléversé et rattaché à l'assurance")
		void logoTeleverse() {
			MediaEntity media = MediaEntity.builder().id("m1").build();
			var logo = new MockMultipartFile("f", "logo.png", "image/png", new byte[]{1});
			when(mediaService.uploadMedia(logo)).thenReturn(Media.builder().id("m1").build());
			when(mediaService.getMediaEntity("m1")).thenReturn(media);
			when(assuranceRepository.save(any(AssuranceEntity.class))).thenAnswer(i -> i.getArgument(0));
			when(assuranceMapper.toDto(any())).thenReturn(Assurance.builder().build());

			service.createAssurance("NSIA", logo);

			ArgumentCaptor<AssuranceEntity> captor = ArgumentCaptor.forClass(AssuranceEntity.class);
			verify(assuranceRepository).save(captor.capture());
			assertThat(captor.getValue().getLibelle()).isEqualTo("NSIA");
			assertThat(captor.getValue().getLogo()).isSameAs(media);
		}

		@Test
		@DisplayName("un logo vide n'est pas téléversé")
		void logoVide() {
			var logo = new MockMultipartFile("f", "logo.png", "image/png", new byte[0]);
			when(assuranceRepository.save(any(AssuranceEntity.class))).thenAnswer(i -> i.getArgument(0));
			when(assuranceMapper.toDto(any())).thenReturn(Assurance.builder().build());

			service.createAssurance("NSIA", logo);

			verify(mediaService, never()).uploadMedia(any());
		}

		@Test
		@DisplayName("mettre à jour une assurance inconnue lève 404")
		void assuranceInconnue() {
			when(assuranceRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.updateAssurance(99L, "NSIA", null))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("la mise à jour sans nouveau logo conserve le logo existant")
		void logoConserve() {
			MediaEntity ancien = MediaEntity.builder().id("m0").build();
			AssuranceEntity entity = AssuranceEntity.builder().id(1L).libelle("Ancien").logo(ancien).build();
			when(assuranceRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(assuranceRepository.save(entity)).thenReturn(entity);
			when(assuranceMapper.toDto(entity)).thenReturn(Assurance.builder().build());

			service.updateAssurance(1L, "NSIA", null);

			assertThat(entity.getLibelle()).isEqualTo("NSIA");
			assertThat(entity.getLogo()).isSameAs(ancien);
		}
	}

	@Nested
	@DisplayName("Référentiels simples : 404 systématique sur id inconnu")
	class ReferentielsSimples {

		@Test
		@DisplayName("catégorie, service, type d'intervention, famille, type de dépense et type d'assurance")
		void quatreCentQuatre() {
			when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
			when(serviceRepository.findById(99L)).thenReturn(Optional.empty());
			when(typeInterventionRepository.findById(99L)).thenReturn(Optional.empty());
			when(familleProduitRepository.findById(99L)).thenReturn(Optional.empty());
			when(typeDepenseRepository.findById(99L)).thenReturn(Optional.empty());
			when(typeAssuranceRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getCategorieById(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.getServiceById(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.getTypeInterventionById(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.getFamilleProduitById(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.getTypeDepenseById(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.getTypeAssuranceById(99L)).isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("les suppressions vérifient l'existence avant d'agir")
		void suppressions() {
			when(categoryRepository.existsById(99L)).thenReturn(false);
			when(serviceRepository.existsById(99L)).thenReturn(false);
			when(typeInterventionRepository.existsById(99L)).thenReturn(false);
			when(familleProduitRepository.existsById(99L)).thenReturn(false);
			when(typeDepenseRepository.existsById(99L)).thenReturn(false);
			when(assuranceRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteCategorie(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.deleteService(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.deleteTypeIntervention(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.deleteFamilleProduit(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.deleteTypeDepense(99L)).isInstanceOf(ResourceNotFoundException.class);
			assertThatThrownBy(() -> service.deleteAssurance(99L)).isInstanceOf(ResourceNotFoundException.class);

			verify(categoryRepository, never()).deleteById(any());
			verify(serviceRepository, never()).deleteById(any());
		}

		@Test
		@DisplayName("les créations de référentiel préservent un id explicite")
		void idsExplicites() {
			ServiceEntity serviceEntity = new ServiceEntity();
			FamilleProduitEntity familleEntity = new FamilleProduitEntity();
			TypeCarburantEntity carburantEntity = new TypeCarburantEntity();

			ServiceRequest serviceRequest = ServiceRequest.builder().id(7L).build();
			FamilleProduitRequest familleRequest = FamilleProduitRequest.builder().id(8L).build();
			TypeCarburantRequest carburantRequest = TypeCarburantRequest.builder().id(9L).build();

			when(serviceMapper.toEntity(serviceRequest)).thenReturn(serviceEntity);
			when(serviceRepository.save(serviceEntity)).thenReturn(serviceEntity);
			when(serviceMapper.toDto(serviceEntity)).thenReturn(new net.ivoireautoservice.ias_manager.dto.core.Service());
			when(familleProduitMapper.toEntity(familleRequest)).thenReturn(familleEntity);
			when(familleProduitRepository.save(familleEntity)).thenReturn(familleEntity);
			when(familleProduitMapper.toDto(familleEntity)).thenReturn(FamilleProduit.builder().build());
			when(typeCarburantMapper.toEntity(carburantRequest)).thenReturn(carburantEntity);
			when(typeCarburantRepository.save(carburantEntity)).thenReturn(carburantEntity);
			when(typeCarburantMapper.toDto(carburantEntity)).thenReturn(TypeCarburant.builder().build());

			service.createService(serviceRequest);
			service.createFamilleProduit(familleRequest);
			service.createTypeCarburant(carburantRequest);

			assertThat(serviceEntity.getId()).isEqualTo(7L);
			assertThat(familleEntity.getId()).isEqualTo(8L);
			assertThat(carburantEntity.getId()).isEqualTo(9L);
		}
	}
}
