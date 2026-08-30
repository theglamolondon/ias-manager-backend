package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.config.MediaProperties;
import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import net.ivoireautoservice.ias_manager.entity.LivraisonClientEntity;
import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import net.ivoireautoservice.ias_manager.entity.MediaEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.PieceJointeEntity;
import net.ivoireautoservice.ias_manager.enums.PieceJointeOwnerTypeEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.PieceJointeMapper;
import net.ivoireautoservice.ias_manager.repository.BonCommandeRepository;
import net.ivoireautoservice.ias_manager.repository.FactureRepository;
import net.ivoireautoservice.ias_manager.repository.LivraisonClientRepository;
import net.ivoireautoservice.ias_manager.repository.LivraisonFournisseurRepository;
import net.ivoireautoservice.ias_manager.repository.MediaRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import net.ivoireautoservice.ias_manager.repository.PieceJointeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
@DisplayName("PieceJointeService — pièces jointes et héritage des droits du propriétaire")
class PieceJointeServiceTest {

	@TempDir
	Path uploadDir;

	@Mock
	private PieceJointeRepository pieceJointeRepository;

	@Mock
	private MediaRepository mediaRepository;

	@Mock
	private PieceJointeMapper pieceJointeMapper;

	@Mock
	private BonCommandeRepository bonCommandeRepository;

	@Mock
	private LivraisonFournisseurRepository livraisonFournisseurRepository;

	@Mock
	private LivraisonClientRepository livraisonClientRepository;

	@Mock
	private FactureRepository factureRepository;

	@Mock
	private PartenaireRepository partenaireRepository;

	private PieceJointeService service;

	@BeforeEach
	void setUp() throws Exception {
		MediaProperties properties = new MediaProperties();
		properties.setUploadDir(uploadDir.toString());
		service = new PieceJointeService(pieceJointeRepository, mediaRepository, pieceJointeMapper,
				properties, bonCommandeRepository, livraisonFournisseurRepository,
				livraisonClientRepository, factureRepository, partenaireRepository);
		service.init();

		when(bonCommandeRepository.findById(1L))
				.thenReturn(Optional.of(BonCommandeEntity.builder().id(1L).build()));
		when(livraisonFournisseurRepository.findById(1L))
				.thenReturn(Optional.of(LivraisonFournisseurEntity.builder().id(1L).build()));
		when(livraisonClientRepository.findById(1L))
				.thenReturn(Optional.of(LivraisonClientEntity.builder().id(1L).build()));
		when(partenaireRepository.findById(1L))
				.thenReturn(Optional.of(PartenaireEntity.builder().id(1L).build()));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private static void authentifierAvec(String... permissions) {
		var autorites = Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("agent", null, autorites));
	}

	private void stubFacture(boolean factureClient) {
		when(factureRepository.findById(1L)).thenReturn(Optional.of(
				FactureEntity.builder().id(1L).factureClient(factureClient).build()));
	}

	@Nested
	@DisplayName("Contrôle d'accès en lecture")
	class Lecture {

		@ParameterizedTest
		@CsvSource({
				"BON_COMMANDE, BON_COMMANDE_READ",
				"LIVRAISON_FOURNISSEUR, APPRO_READ",
				"LIVRAISON_CLIENT, LIVRAISON_CLIENT_READ",
				"PARTENAIRE, PARTENAIRE_READ"
		})
		@DisplayName("chaque type de propriétaire exige sa permission de lecture")
		void permissionRequise(PieceJointeOwnerTypeEnum ownerType, String permission) {
			authentifierAvec(permission);
			when(pieceJointeRepository.findByOwnerTypeAndOwnerId(ownerType, 1L)).thenReturn(List.of());
			when(pieceJointeMapper.toDtoList(any())).thenReturn(List.of());

			assertThat(service.getByOwner(ownerType, 1L)).isEmpty();
		}

		@Test
		@DisplayName("une permission d'un autre module ne donne pas accès (anti-IDOR)")
		void permissionInsuffisante() {
			authentifierAvec("PARTENAIRE_READ");

			assertThatThrownBy(() -> service.getByOwner(PieceJointeOwnerTypeEnum.BON_COMMANDE, 1L))
					.isInstanceOf(AccessDeniedException.class)
					.hasMessageContaining("BON_COMMANDE_READ");
		}

		@Test
		@DisplayName("sans authentification, l'accès est refusé")
		void sansAuthentification() {
			assertThatThrownBy(() -> service.getByOwner(PieceJointeOwnerTypeEnum.BON_COMMANDE, 1L))
					.isInstanceOf(AccessDeniedException.class);
		}

		@Test
		@DisplayName("une pièce d'une facture client exige FACTURE_CLIENT_READ")
		void factureClient() {
			stubFacture(true);
			authentifierAvec("FACTURE_CLIENT_READ");
			when(pieceJointeRepository.findByOwnerTypeAndOwnerId(PieceJointeOwnerTypeEnum.FACTURE, 1L))
					.thenReturn(List.of());
			when(pieceJointeMapper.toDtoList(any())).thenReturn(List.of());

			assertThat(service.getByOwner(PieceJointeOwnerTypeEnum.FACTURE, 1L)).isEmpty();
		}

		@Test
		@DisplayName("FACTURE_FOURNISSEUR_READ ne donne pas accès aux pièces d'une facture client")
		void factureClient_permissionFournisseurInsuffisante() {
			stubFacture(true);
			authentifierAvec("FACTURE_FOURNISSEUR_READ");

			assertThatThrownBy(() -> service.getByOwner(PieceJointeOwnerTypeEnum.FACTURE, 1L))
					.isInstanceOf(AccessDeniedException.class)
					.hasMessageContaining("FACTURE_CLIENT_READ");
		}

		@Test
		@DisplayName("une pièce d'une facture fournisseur exige FACTURE_FOURNISSEUR_READ")
		void factureFournisseur() {
			stubFacture(false);
			authentifierAvec("FACTURE_FOURNISSEUR_READ");
			when(pieceJointeRepository.findByOwnerTypeAndOwnerId(PieceJointeOwnerTypeEnum.FACTURE, 1L))
					.thenReturn(List.of());
			when(pieceJointeMapper.toDtoList(any())).thenReturn(List.of());

			assertThat(service.getByOwner(PieceJointeOwnerTypeEnum.FACTURE, 1L)).isEmpty();
		}

		@Test
		@DisplayName("un propriétaire inexistant lève 404 avant tout contrôle de droits")
		void ownerInconnu() {
			when(bonCommandeRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getByOwner(PieceJointeOwnerTypeEnum.BON_COMMANDE, 99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Bon de commande avec l'id 99");
		}
	}

	@Nested
	@DisplayName("Téléversement")
	class Televersement {

		@ParameterizedTest
		@ValueSource(strings = {"image/png", "image/jpeg", "image/jpg", "application/pdf"})
		@DisplayName("accepte les types autorisés")
		void typesAutorises(String contentType) {
			authentifierAvec("BON_COMMANDE_UPDATE");
			var fichier = new MockMultipartFile("f", "piece.pdf", contentType, new byte[]{1});
			when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(i -> i.getArgument(0));
			when(pieceJointeRepository.save(any(PieceJointeEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.upload(PieceJointeOwnerTypeEnum.BON_COMMANDE, 1L, fichier);

			verify(pieceJointeRepository).save(any(PieceJointeEntity.class));
		}

		@Test
		@DisplayName("refuse un type non autorisé avant tout accès disque")
		void typeRefuse() {
			var fichier = new MockMultipartFile("f", "x.zip", "application/zip", new byte[]{1});

			assertThatThrownBy(() -> service.upload(PieceJointeOwnerTypeEnum.BON_COMMANDE, 1L, fichier))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Type de fichier non autorisé");
			verify(mediaRepository, never()).save(any());
		}

		@Test
		@DisplayName("refuse un fichier vide")
		void fichierVide() {
			var fichier = new MockMultipartFile("f", "vide.pdf", "application/pdf", new byte[0]);

			assertThatThrownBy(() -> service.upload(PieceJointeOwnerTypeEnum.BON_COMMANDE, 1L, fichier))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("vide");
		}

		@Test
		@DisplayName("téléverser exige la permission d'écriture, pas seulement de lecture")
		void permissionEcriture() {
			authentifierAvec("BON_COMMANDE_READ");
			var fichier = new MockMultipartFile("f", "piece.pdf", "application/pdf", new byte[]{1});

			assertThatThrownBy(() -> service.upload(PieceJointeOwnerTypeEnum.BON_COMMANDE, 1L, fichier))
					.isInstanceOf(AccessDeniedException.class)
					.hasMessageContaining("BON_COMMANDE_UPDATE");
		}

		@Test
		@DisplayName("le fichier est écrit sur disque avec un nom d'identifiant unique")
		void ecritureDisque() {
			authentifierAvec("BON_COMMANDE_UPDATE");
			var fichier = new MockMultipartFile("f", "Facture Scan.PDF", "application/pdf", new byte[]{1, 2});
			when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(i -> i.getArgument(0));
			when(pieceJointeRepository.save(any(PieceJointeEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.upload(PieceJointeOwnerTypeEnum.BON_COMMANDE, 1L, fichier);

			var captor = org.mockito.ArgumentCaptor.forClass(MediaEntity.class);
			verify(mediaRepository).save(captor.capture());
			MediaEntity media = captor.getValue();
			assertThat(media.getStoredFilename()).isEqualTo(media.getId() + ".pdf");
			assertThat(media.getOriginalFilename()).isEqualTo("Facture Scan.PDF");
			assertThat(uploadDir.resolve(media.getStoredFilename())).exists();
		}

		@Test
		@DisplayName("ajouter une pièce à une facture client exige FACTURE_CLIENT_CREATE")
		void factureClientEcriture() {
			stubFacture(true);
			authentifierAvec("FACTURE_CLIENT_READ");
			var fichier = new MockMultipartFile("f", "p.pdf", "application/pdf", new byte[]{1});

			assertThatThrownBy(() -> service.upload(PieceJointeOwnerTypeEnum.FACTURE, 1L, fichier))
					.isInstanceOf(AccessDeniedException.class)
					.hasMessageContaining("FACTURE_CLIENT_CREATE");
		}
	}

	@Nested
	@DisplayName("Suppression")
	class Suppression {

		@Test
		@DisplayName("supprime la pièce, le média et le fichier physique")
		void suppressionComplete() throws Exception {
			authentifierAvec("BON_COMMANDE_UPDATE");
			Path fichier = uploadDir.resolve("abc.pdf");
			Files.write(fichier, new byte[]{1});
			MediaEntity media = MediaEntity.builder().id("abc").storedFilename("abc.pdf").build();
			PieceJointeEntity pj = PieceJointeEntity.builder()
					.id(1L).ownerType(PieceJointeOwnerTypeEnum.BON_COMMANDE).ownerId(1L).media(media).build();
			when(pieceJointeRepository.findById(1L)).thenReturn(Optional.of(pj));

			service.delete(1L);

			verify(pieceJointeRepository).delete(pj);
			verify(mediaRepository).delete(media);
			assertThat(fichier).doesNotExist();
		}

		@Test
		@DisplayName("supprimer exige la permission d'écriture du propriétaire")
		void permissionRequise() {
			authentifierAvec("BON_COMMANDE_READ");
			PieceJointeEntity pj = PieceJointeEntity.builder()
					.id(1L).ownerType(PieceJointeOwnerTypeEnum.BON_COMMANDE).ownerId(1L).build();
			when(pieceJointeRepository.findById(1L)).thenReturn(Optional.of(pj));

			assertThatThrownBy(() -> service.delete(1L))
					.isInstanceOf(AccessDeniedException.class);
			verify(pieceJointeRepository, never()).delete(any());
		}

		@Test
		@DisplayName("supprimer une pièce inconnue lève 404")
		void pieceInconnue() {
			when(pieceJointeRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.delete(99L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Pièce jointe avec l'id 99");
		}

		@Test
		@DisplayName("une pièce sans média est supprimée sans erreur")
		void sansMedia() {
			authentifierAvec("BON_COMMANDE_UPDATE");
			PieceJointeEntity pj = PieceJointeEntity.builder()
					.id(1L).ownerType(PieceJointeOwnerTypeEnum.BON_COMMANDE).ownerId(1L).media(null).build();
			when(pieceJointeRepository.findById(1L)).thenReturn(Optional.of(pj));

			service.delete(1L);

			verify(pieceJointeRepository).delete(pj);
			verify(mediaRepository, never()).delete(any());
		}
	}
}
