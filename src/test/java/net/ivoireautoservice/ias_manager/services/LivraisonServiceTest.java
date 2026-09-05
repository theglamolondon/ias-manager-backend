package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.LivraisonClient;
import net.ivoireautoservice.ias_manager.dto.core.LivraisonFournisseur;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonClientRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurItemRequest;
import net.ivoireautoservice.ias_manager.dto.request.LivraisonFournisseurRequest;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.enums.*;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.EntreeProduitMapper;
import net.ivoireautoservice.ias_manager.mapper.LivraisonClientMapper;
import net.ivoireautoservice.ias_manager.mapper.LivraisonFournisseurMapper;
import net.ivoireautoservice.ias_manager.mapper.SortieProduitMapper;
import net.ivoireautoservice.ias_manager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
@DisplayName("LivraisonService — bons de livraison client et fournisseur")
class LivraisonServiceTest {

	@Mock private LivraisonClientRepository livraisonClientRepository;
	@Mock private SortieProduitRepository sortieProduitRepository;
	@Mock private LivraisonFournisseurRepository livraisonFournisseurRepository;
	@Mock private EntreeProduitRepository entreeProduitRepository;
	@Mock private FactureRepository factureRepository;
	@Mock private LigneFactureRepository ligneFactureRepository;
	@Mock private ProduitRepository produitRepository;
	@Mock private BonCommandeRepository bonCommandeRepository;
	@Mock private LigneBonCommandeRepository ligneBonCommandeRepository;
	@Mock private PieceJointeRepository pieceJointeRepository;
	@Mock private LivraisonClientMapper livraisonClientMapper;
	@Mock private SortieProduitMapper sortieProduitMapper;
	@Mock private LivraisonFournisseurMapper livraisonFournisseurMapper;
	@Mock private EntreeProduitMapper entreeProduitMapper;
	@Mock private PrintService printService;
	@Mock private SecurityService securityService;

	@InjectMocks
	private LivraisonService service;

	@BeforeEach
	void setUp() {
		when(livraisonClientRepository.save(any(LivraisonClientEntity.class))).thenAnswer(i -> i.getArgument(0));
		when(livraisonFournisseurRepository.save(any(LivraisonFournisseurEntity.class)))
				.thenAnswer(i -> i.getArgument(0));
		when(entreeProduitRepository.save(any(EntreeProduitEntity.class))).thenAnswer(i -> i.getArgument(0));
		when(sortieProduitRepository.save(any(SortieProduitEntity.class))).thenAnswer(i -> i.getArgument(0));
		when(livraisonClientMapper.toDto(any())).thenReturn(new LivraisonClient());
		when(livraisonFournisseurMapper.toDto(any())).thenReturn(new LivraisonFournisseur());
		when(sortieProduitMapper.toDtoList(any())).thenReturn(List.of());
		when(entreeProduitMapper.toDtoList(any())).thenReturn(List.of());
	}

	private static FactureEntity facture(FactureStatusEnum statut, boolean client, FactureTypeEnum type) {
		return FactureEntity.builder()
				.id(9L).numFacture("DA/01/79/1").numProforma("DA/01/79/1")
				.statut(statut).factureClient(client).type(type).build();
	}

	private static LigneBonCommandeEntity ligneBc(Long id, long qte, Long qteLivree) {
		return LigneBonCommandeEntity.builder()
				.id(id).reference("REF-" + id).designation("Pneu")
				.qte(qte).qteLivree(qteLivree).prixUnitaire(10_000L).remise(0f)
				.produit(ProduitEntity.builder().id(1L).stock(0L).build())
				.build();
	}

	private static BonCommandeEntity bc(BonCommandeStatusEnum statut) {
		return BonCommandeEntity.builder()
				.id(1L).numero("BC-1").statut(statut).tva(18f)
				.partenaire(PartenaireEntity.builder().id(5L).build()).build();
	}

	private static LivraisonFournisseurEntity blf(Long id, StatutBonLivraisonEnum statut, BonCommandeEntity bc) {
		LivraisonFournisseurEntity entity = LivraisonFournisseurEntity.builder()
				.numero("BLF-" + id).statut(statut).bonCommande(bc).build();
		entity.setId(id);
		return entity;
	}

	@Nested
	@DisplayName("Livraison client")
	class Client {

		@ParameterizedTest
		@EnumSource(value = FactureStatusEnum.class, names = {"BROUILLON", "ANNULEE"})
		@DisplayName("seule une facture PROFORMA, FACTUREE ou PAYEE peut être livrée")
		void statutFactureInvalide(FactureStatusEnum statut) {
			when(factureRepository.findById(9L))
					.thenReturn(Optional.of(facture(statut, true, FactureTypeEnum.PRODUIT)));

			assertThatThrownBy(() -> service.enregistrerLivraisonClient(9L, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("PROFORMA, FACTUREE ou PAYEE");
		}

		@Test
		@DisplayName("une facture déjà FACTUREE conserve son statut")
		void factureFactureeConserveStatut() {
			FactureEntity facture = facture(FactureStatusEnum.FACTUREE, true, FactureTypeEnum.PRODUIT);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(facture));
			when(livraisonClientRepository.findByFactureId(9L)).thenReturn(Optional.empty());
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of());

			service.enregistrerLivraisonClient(9L, null);

			assertThat(facture.getStatut()).isEqualTo(FactureStatusEnum.FACTUREE);
			verify(factureRepository, never()).save(any());
		}

		@Test
		@DisplayName("une facture déjà livrée est refusée")
		void dejaLivree() {
			when(factureRepository.findById(9L))
					.thenReturn(Optional.of(facture(FactureStatusEnum.PROFORMA, true, FactureTypeEnum.PRODUIT)));
			when(livraisonClientRepository.findByFactureId(9L))
					.thenReturn(Optional.of(new LivraisonClientEntity()));

			assertThatThrownBy(() -> service.enregistrerLivraisonClient(9L, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà fait l'objet d'une livraison");
		}

		@Test
		@DisplayName("la livraison décrémente le stock des produits livrés")
		void decrementeStock() {
			FactureEntity facture = facture(FactureStatusEnum.PROFORMA, true, FactureTypeEnum.PRODUIT);
			ProduitEntity produit = ProduitEntity.builder().id(1L).stock(50L).build();
			LigneFactureEntity ligne = LigneFactureEntity.builder()
					.id(1L).produit(produit).qte(8L).build();
			when(factureRepository.findById(9L)).thenReturn(Optional.of(facture));
			when(livraisonClientRepository.findByFactureId(9L)).thenReturn(Optional.empty());
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of(ligne));

			service.enregistrerLivraisonClient(9L, null);

			assertThat(produit.getStock()).isEqualTo(42L);
			verify(produitRepository).save(produit);
		}

		@Test
		@DisplayName("une facture PROFORMA passe au statut FACTUREE après livraison")
		void passageAFacturee() {
			FactureEntity facture = facture(FactureStatusEnum.PROFORMA, true, FactureTypeEnum.PRODUIT);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(facture));
			when(livraisonClientRepository.findByFactureId(9L)).thenReturn(Optional.empty());
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of());

			service.enregistrerLivraisonClient(9L, null);

			assertThat(facture.getStatut()).isEqualTo(FactureStatusEnum.FACTUREE);
			verify(factureRepository).save(facture);
		}

		@Test
		@DisplayName("une facture déjà PAYEE conserve son statut")
		void facturePayeeConserveStatut() {
			FactureEntity facture = facture(FactureStatusEnum.PAYEE, true, FactureTypeEnum.PRODUIT);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(facture));
			when(livraisonClientRepository.findByFactureId(9L)).thenReturn(Optional.empty());
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of());

			service.enregistrerLivraisonClient(9L, null);

			assertThat(facture.getStatut()).isEqualTo(FactureStatusEnum.PAYEE);
			verify(factureRepository, never()).save(any());
		}

		@Test
		@DisplayName("sans objet fourni, l'objet reprend la référence de la facture")
		void objetParDefaut() {
			FactureEntity facture = facture(FactureStatusEnum.PAYEE, true, FactureTypeEnum.PRODUIT);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(facture));
			when(livraisonClientRepository.findByFactureId(9L)).thenReturn(Optional.empty());
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of());

			service.enregistrerLivraisonClient(9L, LivraisonClientRequest.builder().objet("  ").build());

			ArgumentCaptor<LivraisonClientEntity> captor =
					ArgumentCaptor.forClass(LivraisonClientEntity.class);
			verify(livraisonClientRepository).save(captor.capture());
			assertThat(captor.getValue().getObjet()).isEqualTo("Livraison DA/01/79/1");
		}

		@Test
		@DisplayName("les lignes sans produit ne génèrent pas de sortie de stock")
		void lignesSansProduit() {
			FactureEntity facture = facture(FactureStatusEnum.PAYEE, true, FactureTypeEnum.PRODUIT);
			LigneFactureEntity ligne = LigneFactureEntity.builder().id(1L).qte(2L).build();
			when(factureRepository.findById(9L)).thenReturn(Optional.of(facture));
			when(livraisonClientRepository.findByFactureId(9L)).thenReturn(Optional.empty());
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of(ligne));

			service.enregistrerLivraisonClient(9L, null);

			verify(sortieProduitRepository, never()).save(any());
			verify(produitRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("Bon de livraison fournisseur depuis une facture")
	class FournisseurDepuisFacture {

		@Test
		@DisplayName("une facture client ne peut pas produire un BL fournisseur")
		void factureClient() {
			when(factureRepository.findById(9L))
					.thenReturn(Optional.of(facture(FactureStatusEnum.PROFORMA, true, FactureTypeEnum.PRODUIT)));

			assertThatThrownBy(() -> service.enregistrerLivraisonFournisseurFromFacture(9L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("depuis une facture client");
		}

		@Test
		@DisplayName("une facture de type MISSION ne peut pas produire un BL fournisseur")
		void factureMission() {
			when(factureRepository.findById(9L))
					.thenReturn(Optional.of(facture(FactureStatusEnum.PROFORMA, false, FactureTypeEnum.MISSION)));

			assertThatThrownBy(() -> service.enregistrerLivraisonFournisseurFromFacture(9L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("type MISSION");
		}

		@Test
		@DisplayName("une facture déjà rattachée à un BL est refusée")
		void dejaLiee() {
			when(factureRepository.findById(9L))
					.thenReturn(Optional.of(facture(FactureStatusEnum.PROFORMA, false, FactureTypeEnum.PRODUIT)));
			when(livraisonFournisseurRepository.existsByFactureId(9L)).thenReturn(true);

			assertThatThrownBy(() -> service.enregistrerLivraisonFournisseurFromFacture(9L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà fait l'objet");
		}

		@Test
		@DisplayName("le BL est créé au statut CREE avec une entrée par ligne produit")
		void creation() {
			FactureEntity facture = facture(FactureStatusEnum.PROFORMA, false, FactureTypeEnum.PRODUIT);
			LigneFactureEntity ligne = LigneFactureEntity.builder()
					.id(1L).produit(ProduitEntity.builder().id(1L).build()).qte(5L).build();
			when(factureRepository.findById(9L)).thenReturn(Optional.of(facture));
			when(livraisonFournisseurRepository.existsByFactureId(9L)).thenReturn(false);
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of(ligne));

			service.enregistrerLivraisonFournisseurFromFacture(9L);

			ArgumentCaptor<LivraisonFournisseurEntity> captor =
					ArgumentCaptor.forClass(LivraisonFournisseurEntity.class);
			verify(livraisonFournisseurRepository).save(captor.capture());
			assertThat(captor.getValue().getStatut()).isEqualTo(StatutBonLivraisonEnum.CREE);
			verify(entreeProduitRepository).save(any(EntreeProduitEntity.class));
		}
	}

	@Nested
	@DisplayName("Création d'un BL fournisseur depuis un bon de commande")
	class CreationDepuisBc {

		private LivraisonFournisseurRequest requete(long quantite) {
			return LivraisonFournisseurRequest.builder()
					.bonCommandeId(1L)
					.items(List.of(LivraisonFournisseurItemRequest.builder()
							.ligneBonCommandeId(1L).quantite(quantite).build()))
					.build();
		}

		@Test
		@DisplayName("le bon de commande est obligatoire")
		void bcObligatoire() {
			assertThatThrownBy(() -> service.createLivraisonFournisseur(
					LivraisonFournisseurRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("bon de commande");
		}

		@Test
		@DisplayName("au moins une ligne livrée est requise")
		void itemsObligatoires() {
			assertThatThrownBy(() -> service.createLivraisonFournisseur(
					LivraisonFournisseurRequest.builder().bonCommandeId(1L).items(List.of()).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Au moins une ligne");
		}

		@ParameterizedTest
		@EnumSource(value = BonCommandeStatusEnum.class, names = {"CREE", "LIVRE", "ANNULE"})
		@DisplayName("seul un BC VALIDE ou PARTIELLEMENT_LIVRE peut être livré")
		void statutBcInvalide(BonCommandeStatusEnum statut) {
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc(statut)));

			assertThatThrownBy(() -> service.createLivraisonFournisseur(requete(1L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("VALIDE ou PARTIELLEMENT_LIVRE");
		}

		@Test
		@DisplayName("une ligne n'appartenant pas au bon de commande est refusée")
		void ligneEtrangere() {
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc(BonCommandeStatusEnum.VALIDE)));
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of());
			when(livraisonFournisseurRepository.findByBonCommandeId(1L)).thenReturn(List.of());

			assertThatThrownBy(() -> service.createLivraisonFournisseur(requete(1L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'appartient pas au bon de commande");
		}

		@Test
		@DisplayName("une quantité dépassant le reste à livrer est refusée")
		void quantiteExcessive() {
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc(BonCommandeStatusEnum.VALIDE)));
			when(ligneBonCommandeRepository.findByBonCommandeId(1L))
					.thenReturn(List.of(ligneBc(1L, 10L, 7L)));
			when(livraisonFournisseurRepository.findByBonCommandeId(1L)).thenReturn(List.of());

			assertThatThrownBy(() -> service.createLivraisonFournisseur(requete(5L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("dépasse le reste à livrer (3)");
		}

		@Test
		@DisplayName("les quantités réservées par les BL en cours réduisent le reste à livrer")
		void quantitesReservees() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LigneBonCommandeEntity ligne = ligneBc(1L, 10L, 0L);
			LivraisonFournisseurEntity blEnCours = blf(2L, StatutBonLivraisonEnum.CREE, bc);
			EntreeProduitEntity reservee = EntreeProduitEntity.builder()
					.id(1L).quantite(8L).ligneBonCommande(ligne).build();
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc));
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of(ligne));
			when(livraisonFournisseurRepository.findByBonCommandeId(1L)).thenReturn(List.of(blEnCours));
			when(entreeProduitRepository.findByLivraisonFournisseurId(2L)).thenReturn(List.of(reservee));

			assertThatThrownBy(() -> service.createLivraisonFournisseur(requete(5L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("dépasse le reste à livrer (2)");
		}

		@Test
		@DisplayName("un BL valide est créé au statut CREE, sans effet sur le stock")
		void creationSansEffetStock() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LigneBonCommandeEntity ligne = ligneBc(1L, 10L, 0L);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc));
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of(ligne));
			when(livraisonFournisseurRepository.findByBonCommandeId(1L)).thenReturn(List.of());

			service.createLivraisonFournisseur(requete(5L));

			ArgumentCaptor<LivraisonFournisseurEntity> captor =
					ArgumentCaptor.forClass(LivraisonFournisseurEntity.class);
			verify(livraisonFournisseurRepository).save(captor.capture());
			assertThat(captor.getValue().getStatut()).isEqualTo(StatutBonLivraisonEnum.CREE);
			assertThat(ligne.getQteLivree()).isZero();
			verify(produitRepository, never()).save(any());
			verify(factureRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("Validation d'un BL fournisseur")
	class Validation {

		private void stubPieceJointe(int nombre) {
			when(pieceJointeRepository.findByOwnerTypeAndOwnerId(
					PieceJointeOwnerTypeEnum.LIVRAISON_FOURNISSEUR, 1L))
					.thenReturn(nombre == 0 ? List.of() : List.of(new PieceJointeEntity()));
		}

		@ParameterizedTest
		@EnumSource(value = StatutBonLivraisonEnum.class, names = {"CREE"}, mode = EnumSource.Mode.EXCLUDE)
		@DisplayName("seul un BL au statut CREE peut être validé")
		void statutInvalide(StatutBonLivraisonEnum statut) {
			when(livraisonFournisseurRepository.findById(1L))
					.thenReturn(Optional.of(blf(1L, statut, bc(BonCommandeStatusEnum.VALIDE))));

			assertThatThrownBy(() -> service.validerLivraisonFournisseur(1L, false))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("statut CREE");
		}

		@Test
		@DisplayName("une pièce jointe est obligatoire avant validation")
		void pieceJointeObligatoire() {
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(
					Optional.of(blf(1L, StatutBonLivraisonEnum.CREE, bc(BonCommandeStatusEnum.VALIDE))));
			stubPieceJointe(0);

			assertThatThrownBy(() -> service.validerLivraisonFournisseur(1L, false))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("pièce jointe est obligatoire");
		}

		@Test
		@DisplayName("la validation incrémente le stock et la quantité livrée")
		void effetsDeLaValidation() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.CREE, bc);
			LigneBonCommandeEntity ligne = ligneBc(1L, 10L, 0L);
			ProduitEntity produit = ligne.getProduit();
			produit.setStock(20L);
			EntreeProduitEntity entree = EntreeProduitEntity.builder()
					.id(1L).quantite(4L).ligneBonCommande(ligne).produit(produit).build();
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			stubPieceJointe(1);
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of(ligne));
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of(entree));

			service.validerLivraisonFournisseur(1L, false);

			assertThat(ligne.getQteLivree()).isEqualTo(4L);
			assertThat(produit.getStock()).isEqualTo(24L);
			assertThat(bl.getStatut()).isEqualTo(StatutBonLivraisonEnum.VALIDE);
			assertThat(bl.getDateValidation()).isNotNull();
		}

		@Test
		@DisplayName("une livraison partielle passe le BC en PARTIELLEMENT_LIVRE")
		void bcPartiellementLivre() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.CREE, bc);
			LigneBonCommandeEntity ligne = ligneBc(1L, 10L, 0L);
			EntreeProduitEntity entree = EntreeProduitEntity.builder()
					.id(1L).quantite(4L).ligneBonCommande(ligne).produit(ligne.getProduit()).build();
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			stubPieceJointe(1);
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of(ligne));
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of(entree));

			service.validerLivraisonFournisseur(1L, false);

			assertThat(bc.getStatut()).isEqualTo(BonCommandeStatusEnum.PARTIELLEMENT_LIVRE);
		}

		@Test
		@DisplayName("une livraison complète passe le BC en LIVRE")
		void bcLivre() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.CREE, bc);
			LigneBonCommandeEntity ligne = ligneBc(1L, 10L, 0L);
			EntreeProduitEntity entree = EntreeProduitEntity.builder()
					.id(1L).quantite(10L).ligneBonCommande(ligne).produit(ligne.getProduit()).build();
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			stubPieceJointe(1);
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of(ligne));
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of(entree));

			service.validerLivraisonFournisseur(1L, false);

			assertThat(bc.getStatut()).isEqualTo(BonCommandeStatusEnum.LIVRE);
		}

		@Test
		@DisplayName("une quantité devenue excessive entre-temps bloque la validation")
		void quantiteObsolete() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.CREE, bc);
			LigneBonCommandeEntity ligne = ligneBc(1L, 10L, 9L);
			EntreeProduitEntity entree = EntreeProduitEntity.builder()
					.id(1L).quantite(4L).ligneBonCommande(ligne).produit(ligne.getProduit()).build();
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			stubPieceJointe(1);
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of(ligne));
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of(entree));

			assertThatThrownBy(() -> service.validerLivraisonFournisseur(1L, false))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Validation impossible");
			assertThat(ligne.getQteLivree()).isEqualTo(9L);
		}

		@Test
		@DisplayName("facturer immédiatement génère une facture fournisseur rattachée au BL")
		void facturationImmediate() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.CREE, bc);
			LigneBonCommandeEntity ligne = ligneBc(1L, 10L, 0L);
			EntreeProduitEntity entree = EntreeProduitEntity.builder()
					.id(1L).quantite(4L).ligneBonCommande(ligne).produit(ligne.getProduit()).build();
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			stubPieceJointe(1);
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of(ligne));
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of(entree));
			when(factureRepository.findMaxNumProformaSuffix("DA/01/79/")).thenReturn(3);
			when(factureRepository.save(any(FactureEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.validerLivraisonFournisseur(1L, true);

			ArgumentCaptor<FactureEntity> captor = ArgumentCaptor.forClass(FactureEntity.class);
			verify(factureRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
			FactureEntity facture = captor.getValue();
			assertThat(facture.getMontantHt()).isEqualTo(40_000L);
			assertThat(facture.getMontantTtc()).isEqualTo(47_200L);
			assertThat(facture.getNumProforma()).isEqualTo("DA/01/79/4");
			assertThat(facture.getNumFacture()).isEqualTo("F-BLF-1");
			assertThat(facture.getFactureClient()).isFalse();
			assertThat(bl.getFacture()).isSameAs(facture);
		}

		@Test
		@DisplayName("sans facturation immédiate, aucune facture n'est créée")
		void sansFacturation() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.CREE, bc);
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			stubPieceJointe(1);
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of());
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of());

			service.validerLivraisonFournisseur(1L, false);

			verify(factureRepository, never()).save(any());
			assertThat(bl.getFacture()).isNull();
		}
	}

	@Nested
	@DisplayName("Annulation d'un BL fournisseur")
	class Annulation {

		@Test
		@DisplayName("un BL déjà annulé ne peut pas l'être à nouveau")
		void dejaAnnule() {
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(
					Optional.of(blf(1L, StatutBonLivraisonEnum.ANNULE, bc(BonCommandeStatusEnum.VALIDE))));

			assertThatThrownBy(() -> service.annulerLivraisonFournisseur(1L, BonCommandeStatusEnum.VALIDE))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà annulé");
		}

		@Test
		@DisplayName("un BL déjà facturé ne peut pas être annulé")
		void dejaFacture() {
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.VALIDE, bc(BonCommandeStatusEnum.LIVRE));
			bl.setFacture(FactureEntity.builder().id(9L).numFacture("F-1").build());
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));

			assertThatThrownBy(() -> service.annulerLivraisonFournisseur(1L, BonCommandeStatusEnum.VALIDE))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("F-1");
		}

		@Test
		@DisplayName("annuler un BL validé défait le stock et la quantité livrée")
		void annulationDefaitLesEffets() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.PARTIELLEMENT_LIVRE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.VALIDE, bc);
			LigneBonCommandeEntity ligne = ligneBc(1L, 10L, 4L);
			ProduitEntity produit = ligne.getProduit();
			produit.setStock(24L);
			EntreeProduitEntity entree = EntreeProduitEntity.builder()
					.id(1L).quantite(4L).ligneBonCommande(ligne).produit(produit).build();
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of(entree));
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of(ligne));

			service.annulerLivraisonFournisseur(1L, BonCommandeStatusEnum.VALIDE);

			assertThat(ligne.getQteLivree()).isZero();
			assertThat(produit.getStock()).isEqualTo(20L);
			assertThat(bl.getStatut()).isEqualTo(StatutBonLivraisonEnum.ANNULE);
			assertThat(bl.getDateAnnulation()).isNotNull();
			assertThat(bc.getStatut()).isEqualTo(BonCommandeStatusEnum.VALIDE);
		}

		@Test
		@DisplayName("annuler un BL non validé ne touche pas au stock")
		void annulationBlCree() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.CREE, bc);
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of());
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of());

			service.annulerLivraisonFournisseur(1L, BonCommandeStatusEnum.LIVRE);

			verify(produitRepository, never()).save(any());
		}

		@Test
		@DisplayName("un statut BC cible incohérent est refusé")
		void statutBcIncoherent() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.CREE, bc);
			LigneBonCommandeEntity ligne = ligneBc(1L, 10L, 0L);
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of());
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of(ligne));

			assertThatThrownBy(() -> service.annulerLivraisonFournisseur(1L, BonCommandeStatusEnum.LIVRE))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Statut BC cible incohérent");
		}

		@Test
		@DisplayName("le statut ANNULE est toujours accepté pour le BC")
		void statutAnnuleToujoursAccepte() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.VALIDE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.CREE, bc);
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of());
			when(ligneBonCommandeRepository.findByBonCommandeId(1L))
					.thenReturn(List.of(ligneBc(1L, 10L, 0L)));

			service.annulerLivraisonFournisseur(1L, BonCommandeStatusEnum.ANNULE);

			assertThat(bc.getStatut()).isEqualTo(BonCommandeStatusEnum.ANNULE);
		}
	}

	@Nested
	@DisplayName("Impression")
	class Impression {

		@Test
		@DisplayName("seul un BL fournisseur VALIDE peut être imprimé")
		void blNonValide() {
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(
					Optional.of(blf(1L, StatutBonLivraisonEnum.CREE, bc(BonCommandeStatusEnum.VALIDE))));

			assertThatThrownBy(() -> service.generateBonLivraisonFournisseurPdf(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("VALIDE peut être imprimé");
		}

		@Test
		@DisplayName("un BL validé sans facture est imprimé depuis ses entrées")
		void blSansFacture() {
			BonCommandeEntity bc = bc(BonCommandeStatusEnum.LIVRE);
			LivraisonFournisseurEntity bl = blf(1L, StatutBonLivraisonEnum.VALIDE, bc);
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(Optional.of(bl));
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of());
			when(printService.generatePdf(any(), any())).thenReturn(new byte[]{1});

			assertThat(service.generateBonLivraisonFournisseurPdf(1L)).isNotEmpty();
			verify(printService).generatePdf(org.mockito.ArgumentMatchers.eq("pdf/BonDeLivraison"), any());
		}
	}

	@Nested
	@DisplayName("Sorties et entrées produit")
	class SortiesEntrees {

		@Test
		@DisplayName("créer une sortie sur une livraison inconnue lève 404")
		void sortieLivraisonInconnue() {
			when(livraisonClientRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createSortieProduit(99L,
					net.ivoireautoservice.ias_manager.dto.request.SortieProduitRequest.builder()
							.produitId(1L).quantite(1L).build()))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Livraison client");
		}

		@Test
		@DisplayName("créer une entrée sur un produit inconnu lève 404")
		void entreeProduitInconnu() {
			when(livraisonFournisseurRepository.findById(1L)).thenReturn(
					Optional.of(blf(1L, StatutBonLivraisonEnum.CREE, bc(BonCommandeStatusEnum.VALIDE))));
			when(produitRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createEntreeProduit(1L,
					net.ivoireautoservice.ias_manager.dto.request.EntreeProduitRequest.builder()
							.produitId(99L).quantite(1L).build()))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Produit avec l'id 99");
		}

		@Test
		@DisplayName("supprimer une entrée sur une livraison inconnue lève 404")
		void suppressionEntreeLivraisonInconnue() {
			when(livraisonFournisseurRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteEntreeProduit(99L, 1L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(entreeProduitRepository, never()).deleteById(any());
		}
	}
}
