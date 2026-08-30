package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.EntreeStock;
import net.ivoireautoservice.ias_manager.dto.core.Media;
import net.ivoireautoservice.ias_manager.dto.core.Produit;
import net.ivoireautoservice.ias_manager.dto.request.EntreeStockRequest;
import net.ivoireautoservice.ias_manager.dto.request.ProduitRequest;
import net.ivoireautoservice.ias_manager.entity.EntreeProduitEntity;
import net.ivoireautoservice.ias_manager.entity.FamilleProduitEntity;
import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import net.ivoireautoservice.ias_manager.entity.MediaEntity;
import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.EntreeProduitMapper;
import net.ivoireautoservice.ias_manager.mapper.LivraisonFournisseurMapper;
import net.ivoireautoservice.ias_manager.mapper.ProduitMapper;
import net.ivoireautoservice.ias_manager.repository.EntreeProduitRepository;
import net.ivoireautoservice.ias_manager.repository.FamilleProduitRepository;
import net.ivoireautoservice.ias_manager.repository.LivraisonFournisseurRepository;
import net.ivoireautoservice.ias_manager.repository.ProduitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

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
@DisplayName("ProduitService — catalogue produits et entrées de stock")
class ProduitServiceTest {

	@Mock
	private ProduitRepository produitRepository;

	@Mock
	private FamilleProduitRepository familleProduitRepository;

	@Mock
	private LivraisonFournisseurRepository livraisonFournisseurRepository;

	@Mock
	private EntreeProduitRepository entreeProduitRepository;

	@Mock
	private MediaService mediaService;

	@Mock
	private ProduitMapper produitMapper;

	@Mock
	private EntreeProduitMapper entreeProduitMapper;

	@Mock
	private LivraisonFournisseurMapper livraisonFournisseurMapper;

	@InjectMocks
	private ProduitService service;

	private final Pageable pageable = PageRequest.of(0, 10);

	@Nested
	@DisplayName("Catalogue")
	class Catalogue {

		@Test
		@DisplayName("un mot-clé déclenche la recherche, détrimé")
		void recherche() {
			when(produitRepository.searchByKeyword("pneu", pageable))
					.thenReturn(new PageImpl<>(List.of(), pageable, 0));

			service.getAllProduits("  pneu ", pageable);

			verify(produitRepository).searchByKeyword("pneu", pageable);
		}

		@Test
		@DisplayName("sans mot-clé, la liste complète est paginée")
		void sansMotCle() {
			when(produitRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

			service.getAllProduits(null, pageable);

			verify(produitRepository).findAll(pageable);
		}

		@Test
		@DisplayName("getProduitById lève 404 sur un id inconnu")
		void parId_absent() {
			when(produitRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getProduitById(99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Produit avec l'id 99");
		}

		@Test
		@DisplayName("getProduitByReference lève 404 sur une référence inconnue")
		void parReference_absent() {
			when(produitRepository.findByReference("REF-404")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getProduitByReference("REF-404"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("REF-404");
		}

		@Test
		@DisplayName("lister par famille inconnue lève 404")
		void parFamille_absente() {
			when(familleProduitRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.getProduitsByFamille(99L, pageable))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(produitRepository, never()).findByFamilleId(any(), any());
		}
	}

	@Nested
	@DisplayName("Création et modification")
	class CreationModification {

		@Test
		@DisplayName("la famille est obligatoire et résolue")
		void create_familleResolue() {
			FamilleProduitEntity famille = FamilleProduitEntity.builder().id(3L).build();
			ProduitRequest request = ProduitRequest.builder().reference("REF-1").familleId(3L).build();
			ProduitEntity entity = new ProduitEntity();
			when(familleProduitRepository.findById(3L)).thenReturn(Optional.of(famille));
			when(produitMapper.toEntity(request)).thenReturn(entity);
			when(produitRepository.save(entity)).thenReturn(entity);
			when(produitMapper.toDto(entity)).thenReturn(Produit.builder().build());

			service.createProduit(request, null);

			assertThat(entity.getFamille()).isSameAs(famille);
		}

		@Test
		@DisplayName("une famille inconnue lève 404 avant toute écriture")
		void create_familleInconnue() {
			ProduitRequest request = ProduitRequest.builder().familleId(99L).build();
			when(familleProduitRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createProduit(request, null))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(produitRepository, never()).save(any());
		}

		@Test
		@DisplayName("une image fournie est téléversée et rattachée au produit")
		void create_avecImage() {
			FamilleProduitEntity famille = FamilleProduitEntity.builder().id(3L).build();
			MediaEntity media = MediaEntity.builder().id("media-1").build();
			ProduitRequest request = ProduitRequest.builder().familleId(3L).build();
			ProduitEntity entity = new ProduitEntity();
			var image = new MockMultipartFile("image", "p.png", "image/png", new byte[]{1});
			when(familleProduitRepository.findById(3L)).thenReturn(Optional.of(famille));
			when(produitMapper.toEntity(request)).thenReturn(entity);
			when(mediaService.uploadMedia(image)).thenReturn(Media.builder().id("media-1").build());
			when(mediaService.getMediaEntity("media-1")).thenReturn(media);
			when(produitRepository.save(entity)).thenReturn(entity);
			when(produitMapper.toDto(entity)).thenReturn(Produit.builder().build());

			service.createProduit(request, image);

			assertThat(entity.getImage()).isSameAs(media);
		}

		@Test
		@DisplayName("une image vide n'est pas téléversée")
		void create_imageVide() {
			FamilleProduitEntity famille = FamilleProduitEntity.builder().id(3L).build();
			ProduitRequest request = ProduitRequest.builder().familleId(3L).build();
			ProduitEntity entity = new ProduitEntity();
			var image = new MockMultipartFile("image", "p.png", "image/png", new byte[0]);
			when(familleProduitRepository.findById(3L)).thenReturn(Optional.of(famille));
			when(produitMapper.toEntity(request)).thenReturn(entity);
			when(produitRepository.save(entity)).thenReturn(entity);
			when(produitMapper.toDto(entity)).thenReturn(Produit.builder().build());

			service.createProduit(request, image);

			verify(mediaService, never()).uploadMedia(any());
			assertThat(entity.getImage()).isNull();
		}

		@Test
		@DisplayName("updateProduit applique la requête et réaffecte la famille")
		void update() {
			ProduitEntity entity = ProduitEntity.builder().id(1L).build();
			FamilleProduitEntity famille = FamilleProduitEntity.builder().id(3L).build();
			ProduitRequest request = ProduitRequest.builder().familleId(3L).build();
			when(produitRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(familleProduitRepository.findById(3L)).thenReturn(Optional.of(famille));
			when(produitRepository.save(entity)).thenReturn(entity);
			when(produitMapper.toDto(entity)).thenReturn(Produit.builder().build());

			service.updateProduit(1L, request, null);

			verify(produitMapper).updateEntity(request, entity);
			assertThat(entity.getFamille()).isSameAs(famille);
		}

		@Test
		@DisplayName("supprimer un produit inconnu lève 404")
		void delete_absent() {
			when(produitRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteProduit(99L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(produitRepository, never()).deleteById(any());
		}
	}

	@Nested
	@DisplayName("Entrée de stock")
	class EntreeDeStock {

		@Test
		@DisplayName("le stock de chaque produit est incrémenté de la quantité reçue")
		void incrementeLeStock() {
			ProduitEntity produit = ProduitEntity.builder().id(1L).stock(40L).build();
			EntreeStockRequest request = EntreeStockRequest.builder()
					.numeroLivraison("BL-1")
					.lignes(List.of(EntreeStockRequest.LigneEntree.builder()
							.produitId(1L).quantite(10L).build()))
					.build();
			when(livraisonFournisseurRepository.save(any(LivraisonFournisseurEntity.class)))
					.thenAnswer(i -> i.getArgument(0));
			when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
			when(entreeProduitRepository.save(any(EntreeProduitEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.enregistrerEntreeStock(request);

			assertThat(produit.getStock()).isEqualTo(50L);
			verify(produitRepository).save(produit);
		}

		@Test
		@DisplayName("un stock initialement nul est traité comme zéro")
		void stockNull() {
			ProduitEntity produit = ProduitEntity.builder().id(1L).stock(null).build();
			EntreeStockRequest request = EntreeStockRequest.builder()
					.lignes(List.of(EntreeStockRequest.LigneEntree.builder()
							.produitId(1L).quantite(7L).build()))
					.build();
			when(livraisonFournisseurRepository.save(any(LivraisonFournisseurEntity.class)))
					.thenAnswer(i -> i.getArgument(0));
			when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
			when(entreeProduitRepository.save(any(EntreeProduitEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.enregistrerEntreeStock(request);

			assertThat(produit.getStock()).isEqualTo(7L);
		}

		@Test
		@DisplayName("sans date fournie, la livraison est horodatée au moment de l'enregistrement")
		void dateParDefaut() {
			EntreeStockRequest request = EntreeStockRequest.builder()
					.numeroLivraison("BL-1").lignes(List.of()).build();
			when(livraisonFournisseurRepository.save(any(LivraisonFournisseurEntity.class)))
					.thenAnswer(i -> i.getArgument(0));

			service.enregistrerEntreeStock(request);

			ArgumentCaptor<LivraisonFournisseurEntity> captor =
					ArgumentCaptor.forClass(LivraisonFournisseurEntity.class);
			verify(livraisonFournisseurRepository).save(captor.capture());
			assertThat(captor.getValue().getDhmsLivraison()).isNotNull();
			assertThat(captor.getValue().getNumero()).isEqualTo("BL-1");
		}

		@Test
		@DisplayName("la date fournie est conservée telle quelle")
		void dateFournie() {
			LocalDateTime date = LocalDateTime.of(2026, 3, 1, 8, 30);
			EntreeStockRequest request = EntreeStockRequest.builder()
					.dhmsLivraison(date).lignes(List.of()).build();
			when(livraisonFournisseurRepository.save(any(LivraisonFournisseurEntity.class)))
					.thenAnswer(i -> i.getArgument(0));

			service.enregistrerEntreeStock(request);

			ArgumentCaptor<LivraisonFournisseurEntity> captor =
					ArgumentCaptor.forClass(LivraisonFournisseurEntity.class);
			verify(livraisonFournisseurRepository).save(captor.capture());
			assertThat(captor.getValue().getDhmsLivraison()).isEqualTo(date);
		}

		@Test
		@DisplayName("un produit inconnu interrompt l'entrée de stock")
		void produitInconnu() {
			EntreeStockRequest request = EntreeStockRequest.builder()
					.lignes(List.of(EntreeStockRequest.LigneEntree.builder()
							.produitId(99L).quantite(1L).build()))
					.build();
			when(livraisonFournisseurRepository.save(any(LivraisonFournisseurEntity.class)))
					.thenAnswer(i -> i.getArgument(0));
			when(produitRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.enregistrerEntreeStock(request))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Produit avec l'id 99");
			verify(entreeProduitRepository, never()).save(any());
		}

		@Test
		@DisplayName("la réponse expose la livraison et les entrées créées")
		void reponse() {
			ProduitEntity produit = ProduitEntity.builder().id(1L).stock(0L).build();
			EntreeStockRequest request = EntreeStockRequest.builder()
					.lignes(List.of(EntreeStockRequest.LigneEntree.builder()
							.produitId(1L).quantite(3L).build()))
					.build();
			when(livraisonFournisseurRepository.save(any(LivraisonFournisseurEntity.class)))
					.thenAnswer(i -> i.getArgument(0));
			when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));
			when(entreeProduitRepository.save(any(EntreeProduitEntity.class))).thenAnswer(i -> i.getArgument(0));
			when(livraisonFournisseurMapper.toDto(any())).thenReturn(new net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseur());
			when(entreeProduitMapper.toDtoList(any())).thenReturn(List.of());

			EntreeStock resultat = service.enregistrerEntreeStock(request);

			assertThat(resultat.getLivraison()).isNotNull();
			assertThat(resultat.getEntrees()).isNotNull();
		}
	}
}
