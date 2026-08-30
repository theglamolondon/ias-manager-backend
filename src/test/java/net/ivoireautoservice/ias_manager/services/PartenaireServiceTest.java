package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.BonCommande;
import net.ivoireautoservice.ias_manager.dto.core.Partenaire;
import net.ivoireautoservice.ias_manager.dto.request.BonCommandeRequest;
import net.ivoireautoservice.ias_manager.dto.request.PartenaireRequest;
import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.BonCommandeMapper;
import net.ivoireautoservice.ias_manager.mapper.PartenaireMapper;
import net.ivoireautoservice.ias_manager.repository.BonCommandeRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@DisplayName("PartenaireService — clients, fournisseurs et leurs bons de commande")
class PartenaireServiceTest {

	@Mock
	private PartenaireRepository partenaireRepository;

	@Mock
	private BonCommandeRepository bonCommandeRepository;

	@Mock
	private PartenaireMapper partenaireMapper;

	@Mock
	private BonCommandeMapper bonCommandeMapper;

	@InjectMocks
	private PartenaireService service;

	private final Pageable pageable = PageRequest.of(0, 10);

	@Test
	@DisplayName("un mot-clé déclenche la recherche, détrimé")
	void getAll_avecMotCle() {
		PartenaireEntity entity = PartenaireEntity.builder().id(1L).build();
		when(partenaireRepository.searchByKeyword("total", pageable))
				.thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
		when(partenaireMapper.toDto(entity)).thenReturn(Partenaire.builder().build());

		service.getAllPartenaires(" total ", pageable);

		verify(partenaireRepository).searchByKeyword("total", pageable);
		verify(partenaireRepository, never()).findAll(any(Pageable.class));
	}

	@Test
	@DisplayName("un mot-clé vide retombe sur findAll")
	void getAll_sansMotCle() {
		when(partenaireRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

		service.getAllPartenaires("  ", pageable);

		verify(partenaireRepository).findAll(pageable);
	}

	@Test
	@DisplayName("getPartenaireById lève 404 sur un id inconnu")
	void getById_absent() {
		when(partenaireRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getPartenaireById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Partenaire avec l'id 99");
	}

	@Test
	@DisplayName("updatePartenaire applique la requête sur l'entité chargée")
	void update() {
		PartenaireEntity entity = PartenaireEntity.builder().id(1L).build();
		PartenaireRequest request = PartenaireRequest.builder().raisonSociale("Total CI").build();
		when(partenaireRepository.findById(1L)).thenReturn(Optional.of(entity));
		when(partenaireRepository.save(entity)).thenReturn(entity);
		when(partenaireMapper.toDto(entity)).thenReturn(Partenaire.builder().build());

		service.updatePartenaire(1L, request);

		verify(partenaireMapper).updateEntity(request, entity);
	}

	@Test
	@DisplayName("la suppression d'un partenaire inconnu lève 404")
	void delete_absent() {
		when(partenaireRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> service.deletePartenaire(99L))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(partenaireRepository, never()).deleteById(any());
	}

	@Test
	@DisplayName("lister les bons de commande d'un partenaire inconnu lève 404")
	void bonsCommande_partenaireInconnu() {
		when(partenaireRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> service.getBonsCommandeByPartenaire(99L, pageable))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(bonCommandeRepository, never()).findByPartenaireId(any(), any());
	}

	@Test
	@DisplayName("un bon de commande est rattaché au partenaire à la création")
	void createBonCommande() {
		PartenaireEntity partenaire = PartenaireEntity.builder().id(1L).build();
		BonCommandeRequest request = BonCommandeRequest.builder().objet("Pneus").build();
		BonCommandeEntity entity = new BonCommandeEntity();
		when(partenaireRepository.findById(1L)).thenReturn(Optional.of(partenaire));
		when(bonCommandeMapper.toEntity(request)).thenReturn(entity);
		when(bonCommandeRepository.save(entity)).thenReturn(entity);
		when(bonCommandeMapper.toDto(entity)).thenReturn(BonCommande.builder().build());

		service.createBonCommande(1L, request);

		assertThat(entity.getPartenaire()).isSameAs(partenaire);
	}

	@Test
	@DisplayName("créer un bon de commande sur un partenaire inconnu lève 404")
	void createBonCommande_partenaireInconnu() {
		when(partenaireRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createBonCommande(99L, BonCommandeRequest.builder().build()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("récupérer un bon inexistant sur un partenaire valide lève 404")
	void getBonCommandeById_bonInconnu() {
		when(partenaireRepository.existsById(1L)).thenReturn(true);
		when(bonCommandeRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getBonCommandeById(1L, 99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Bon de commande avec l'id 99");
	}

	@Test
	@DisplayName("supprimer un bon vérifie d'abord l'existence du partenaire puis du bon")
	void deleteBonCommande_bonInconnu() {
		when(partenaireRepository.existsById(1L)).thenReturn(true);
		when(bonCommandeRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> service.deleteBonCommande(1L, 99L))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(bonCommandeRepository, never()).deleteById(any());
	}

	@Test
	@DisplayName("les listes clients et fournisseurs délèguent aux requêtes dédiées")
	void clientsEtFournisseurs() {
		when(partenaireRepository.findByIsClientTrue(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));
		when(partenaireRepository.findByIsFournisseurTrue(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

		assertThat(service.getClients(pageable).getContent()).isEmpty();
		assertThat(service.getFournisseurs(pageable).getContent()).isEmpty();
	}
}
