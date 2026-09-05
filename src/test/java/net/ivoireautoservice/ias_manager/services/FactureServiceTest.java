package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.config.MoneyUtils;
import net.ivoireautoservice.ias_manager.dto.core.Facture;
import net.ivoireautoservice.ias_manager.dto.request.*;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.enums.*;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.*;
import net.ivoireautoservice.ias_manager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FactureService — numérotation, statuts, avoirs et facturation groupée")
class FactureServiceTest {

	@Mock private FactureRepository factureRepository;
	@Mock private LigneFactureRepository ligneFactureRepository;
	@Mock private LivraisonClientRepository livraisonClientRepository;
	@Mock private LivraisonFournisseurRepository livraisonFournisseurRepository;
	@Mock private SortieProduitRepository sortieProduitRepository;
	@Mock private EntreeProduitRepository entreeProduitRepository;
	@Mock private PartenaireRepository partenaireRepository;
	@Mock private ProduitRepository produitRepository;
	@Mock private LigneCompteRepository ligneCompteRepository;
	@Mock private MissionRepository missionRepository;
	@Mock private FactureMapper factureMapper;
	@Mock private LigneFactureMapper ligneFactureMapper;
	@Mock private LivraisonClientMapper livraisonClientMapper;
	@Mock private LivraisonFournisseurMapper livraisonFournisseurMapper;
	@Mock private SortieProduitMapper sortieProduitMapper;
	@Mock private EntreeProduitMapper entreeProduitMapper;
	@Mock private CompteService compteService;
	@Mock private PrintService printService;
	@Mock private MoneyUtils moneyUtils;
	@Mock private SecurityService securityService;

	@InjectMocks
	private FactureService service;

	@BeforeEach
	void setUp() {
		when(factureMapper.toDto(any(FactureEntity.class))).thenAnswer(i -> {
			FactureEntity e = i.getArgument(0);
			return Facture.builder().id(e.getId()).montantTtc(e.getMontantTtc()).build();
		});
		when(ligneFactureRepository.findByFactureId(any())).thenReturn(List.of());
		when(ligneFactureMapper.toDtoList(any())).thenReturn(List.of());
		when(livraisonClientRepository.findByFactureId(any())).thenReturn(Optional.empty());
		when(livraisonFournisseurRepository.findAllByFactureId(any())).thenReturn(List.of());
		when(factureRepository.save(any(FactureEntity.class))).thenAnswer(i -> i.getArgument(0));
		when(ligneFactureRepository.save(any(LigneFactureEntity.class))).thenAnswer(i -> i.getArgument(0));
	}

	private static PartenaireEntity partenaire(TypePartenaireEnum type) {
		return PartenaireEntity.builder().id(5L).raisonSociale("Client X").type(type).build();
	}

	private static FactureEntity facture(FactureStatusEnum statut, FactureNatureEnum nature, boolean client) {
		return FactureEntity.builder()
				.id(9L).numProforma("DA/01/79/1").numFacture("DA/01/79/1")
				.statut(statut).nature(nature).factureClient(client)
				.montantHt(100_000L).montantTtc(118_000L).tva(18f).objet("Objet")
				.build();
	}

	@Nested
	@DisplayName("Numérotation et nature à la création")
	class Creation {

		private FactureEntity creer(FactureRequest request, FactureTypeEnum type, PartenaireEntity partenaire) {
			FactureEntity entity = new FactureEntity();
			when(factureMapper.toEntity(request)).thenReturn(entity);
			if (partenaire != null) {
				when(partenaireRepository.findById(5L)).thenReturn(Optional.of(partenaire));
			}
			when(factureRepository.findMaxNumProformaSuffix("DA/01/79/")).thenReturn(41);
			service.createFacture(request, type);
			return entity;
		}

		@Test
		@DisplayName("une facture client reçoit le même numéro en proforma et en facture")
		void factureClientNumerotation() {
			FactureRequest request = FactureRequest.builder()
					.factureClient(true).partenaireId(5L).build();

			FactureEntity entity = creer(request, FactureTypeEnum.PRODUIT,
					partenaire(TypePartenaireEnum.ENTREPRISE));

			assertThat(entity.getNumProforma()).isEqualTo("DA/01/79/42");
			assertThat(entity.getNumFacture()).isEqualTo("DA/01/79/42");
			assertThat(entity.getStatut()).isEqualTo(FactureStatusEnum.PROFORMA);
		}

		@Test
		@DisplayName("le numéro fourni pour une facture client est ignoré au profit de la séquence")
		void numeroClientForce() {
			FactureRequest request = FactureRequest.builder()
					.factureClient(true).partenaireId(5L)
					.numProforma("PERSO-1").numFacture("PERSO-1").build();

			FactureEntity entity = creer(request, FactureTypeEnum.PRODUIT,
					partenaire(TypePartenaireEnum.ENTREPRISE));

			assertThat(entity.getNumProforma()).isEqualTo("DA/01/79/42");
		}

		@Test
		@DisplayName("une facture fournisseur ne se voit attribuer un proforma que s'il est absent")
		void proformaFournisseur() {
			FactureRequest request = FactureRequest.builder()
					.factureClient(false).partenaireId(5L).numFacture("F-EXTERNE").build();

			FactureEntity entity = creer(request, FactureTypeEnum.PRODUIT,
					partenaire(TypePartenaireEnum.ENTREPRISE));

			assertThat(entity.getNumProforma()).isEqualTo("DA/01/79/42");
			assertThat(entity.getNumFacture()).isNull();
		}

		@Test
		@DisplayName("la séquence démarre à 1 quand aucune facture n'existe")
		void premiereSequence() {
			FactureRequest request = FactureRequest.builder().factureClient(true).partenaireId(5L).build();
			FactureEntity entity = new FactureEntity();
			when(factureMapper.toEntity(request)).thenReturn(entity);
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(partenaire(TypePartenaireEnum.ENTREPRISE)));
			when(factureRepository.findMaxNumProformaSuffix("DA/01/79/")).thenReturn(null);

			service.createFacture(request, FactureTypeEnum.PRODUIT);

			assertThat(entity.getNumProforma()).isEqualTo("DA/01/79/1");
		}

		@Test
		@DisplayName("un client PARTICULIER donne une facture de nature REÇU")
		void naturePourParticulier() {
			FactureRequest request = FactureRequest.builder().factureClient(true).partenaireId(5L).build();

			FactureEntity entity = creer(request, FactureTypeEnum.MISSION,
					partenaire(TypePartenaireEnum.PARTICULIER));

			assertThat(entity.getNature()).isEqualTo(FactureNatureEnum.RECU);
		}

		@Test
		@DisplayName("un client ENTREPRISE donne une facture de nature FACTURE")
		void naturePourEntreprise() {
			FactureRequest request = FactureRequest.builder().factureClient(true).partenaireId(5L).build();

			FactureEntity entity = creer(request, FactureTypeEnum.MISSION,
					partenaire(TypePartenaireEnum.ENTREPRISE));

			assertThat(entity.getNature()).isEqualTo(FactureNatureEnum.FACTURE);
		}

		@Test
		@DisplayName("une facture fournisseur reste de nature FACTURE même pour un particulier")
		void natureFournisseur() {
			FactureRequest request = FactureRequest.builder().factureClient(false).partenaireId(5L).build();

			FactureEntity entity = creer(request, FactureTypeEnum.PRODUIT,
					partenaire(TypePartenaireEnum.PARTICULIER));

			assertThat(entity.getNature()).isEqualTo(FactureNatureEnum.FACTURE);
		}

		@Test
		@DisplayName("le type PRODUIT est déduit de la présence d'un produit sur au moins une ligne")
		void typeProduit() {
			FactureRequest request = FactureRequest.builder()
					.factureClient(true).partenaireId(5L)
					.items(List.of(LigneFactureRequest.builder().produitId(3L).designation("Pneu").build()))
					.build();
			when(ligneFactureMapper.toEntity(any())).thenReturn(new LigneFactureEntity());
			when(produitRepository.findById(3L)).thenReturn(Optional.of(ProduitEntity.builder().id(3L).build()));

			FactureEntity entity = creer(request, FactureTypeEnum.PRODUIT,
					partenaire(TypePartenaireEnum.ENTREPRISE));

			assertThat(entity.getType()).isEqualTo(FactureTypeEnum.PRODUIT);
		}

		@Test
		@DisplayName("sans produit sur les lignes, le type devient AUTRE")
		void typeAutre() {
			FactureRequest request = FactureRequest.builder()
					.factureClient(true).partenaireId(5L)
					.items(List.of(LigneFactureRequest.builder().designation("Prestation").build()))
					.build();
			when(ligneFactureMapper.toEntity(any())).thenReturn(new LigneFactureEntity());

			FactureEntity entity = creer(request, FactureTypeEnum.PRODUIT,
					partenaire(TypePartenaireEnum.ENTREPRISE));

			assertThat(entity.getType()).isEqualTo(FactureTypeEnum.AUTRE);
		}

		@Test
		@DisplayName("le type MISSION n'est jamais requalifié")
		void typeMissionPreserve() {
			FactureRequest request = FactureRequest.builder()
					.factureClient(true).partenaireId(5L)
					.items(List.of(LigneFactureRequest.builder().designation("Location").build()))
					.build();
			when(ligneFactureMapper.toEntity(any())).thenReturn(new LigneFactureEntity());

			FactureEntity entity = creer(request, FactureTypeEnum.MISSION,
					partenaire(TypePartenaireEnum.ENTREPRISE));

			assertThat(entity.getType()).isEqualTo(FactureTypeEnum.MISSION);
		}

		@Test
		@DisplayName("un partenaire inconnu lève 404")
		void partenaireInconnu() {
			FactureRequest request = FactureRequest.builder().factureClient(true).partenaireId(99L).build();
			when(factureMapper.toEntity(request)).thenReturn(new FactureEntity());
			when(partenaireRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createFacture(request))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("Transitions de statut")
	class Transitions {

		@ParameterizedTest
		@CsvSource({
				"BROUILLON, PROFORMA",
				"BROUILLON, ANNULEE",
				"PROFORMA, FACTUREE",
				"PROFORMA, ANNULEE",
				"FACTUREE, ANNULEE"
		})
		@DisplayName("les transitions autorisées sont appliquées")
		void transitionsValides(FactureStatusEnum de, FactureStatusEnum vers) {
			FactureEntity entity = facture(de, FactureNatureEnum.FACTURE, true);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));

			service.changerStatut(9L, vers, null);

			assertThat(entity.getStatut()).isEqualTo(vers);
		}

		@ParameterizedTest
		@CsvSource({
				"BROUILLON, FACTUREE",
				"BROUILLON, PAYEE",
				"FACTUREE, PROFORMA",
				"PAYEE, ANNULEE",
				"ANNULEE, PROFORMA",
				"PROFORMA, BROUILLON"
		})
		@DisplayName("les transitions interdites sont rejetées")
		void transitionsInvalides(FactureStatusEnum de, FactureStatusEnum vers) {
			FactureEntity entity = facture(de, FactureNatureEnum.FACTURE, true);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.changerStatut(9L, vers, 3L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Transition de statut invalide");
			assertThat(entity.getStatut()).isEqualTo(de);
		}

		@Test
		@DisplayName("passer une facture à PAYEE exige un compte")
		void payeeSansCompte() {
			FactureEntity entity = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.FACTURE, true);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.changerStatut(9L, FactureStatusEnum.PAYEE, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("compte est obligatoire");
		}

		@Test
		@DisplayName("le paiement d'une facture client crée un APPROVISIONNEMENT sur le compte")
		void paiementFactureClient() {
			FactureEntity entity = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.FACTURE, true);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));
			when(compteService.createLigneEntity(eq(3L), any(LigneCompteRequest.class), eq(LigneCompteOrigine.FACTURE)))
					.thenReturn(new LigneCompteEntity());

			service.changerStatut(9L, FactureStatusEnum.PAYEE, 3L);

			ArgumentCaptor<LigneCompteRequest> captor = ArgumentCaptor.forClass(LigneCompteRequest.class);
			verify(compteService).createLigneEntity(eq(3L), captor.capture(), eq(LigneCompteOrigine.FACTURE));
			assertThat(captor.getValue().getType()).isEqualTo(CompteLigneType.APPROVISIONNEMENT);
			assertThat(captor.getValue().getMontant()).isEqualTo(118_000L);
			assertThat(captor.getValue().getObjet()).startsWith("ENCAISSEMENT FACTURE");
			assertThat(entity.getStatut()).isEqualTo(FactureStatusEnum.PAYEE);
		}

		@Test
		@DisplayName("le paiement d'une facture fournisseur crée une DEPENSE sur le compte")
		void paiementFactureFournisseur() {
			FactureEntity entity = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.FACTURE, false);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));
			when(compteService.createLigneEntity(eq(3L), any(LigneCompteRequest.class), eq(LigneCompteOrigine.FACTURE)))
					.thenReturn(new LigneCompteEntity());

			service.changerStatut(9L, FactureStatusEnum.PAYEE, 3L);

			ArgumentCaptor<LigneCompteRequest> captor = ArgumentCaptor.forClass(LigneCompteRequest.class);
			verify(compteService).createLigneEntity(eq(3L), captor.capture(), eq(LigneCompteOrigine.FACTURE));
			assertThat(captor.getValue().getType()).isEqualTo(CompteLigneType.DEPENSE);
			assertThat(captor.getValue().getObjet()).startsWith("PAIEMENT FACTURE");
		}

		@Test
		@DisplayName("le statut d'un avoir ne peut pas être modifié")
		void avoirImmuable() {
			FactureEntity entity = facture(FactureStatusEnum.FACTUREE, FactureNatureEnum.AVOIR, true);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.changerStatut(9L, FactureStatusEnum.PAYEE, 3L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("facture d'avoir");
		}

		@Test
		@DisplayName("annuler une facture fournisseur liée à des bons de livraison est refusé")
		void annulationAvecLivraisonsLiees() {
			FactureEntity entity = facture(FactureStatusEnum.FACTUREE, FactureNatureEnum.FACTURE, false);
			LivraisonFournisseurEntity bl = LivraisonFournisseurEntity.builder().numero("BLF-1").build();
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));
			when(livraisonFournisseurRepository.findAllByFactureId(9L)).thenReturn(List.of(bl));

			assertThatThrownBy(() -> service.changerStatut(9L, FactureStatusEnum.ANNULEE, null))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("BLF-1");
		}
	}

	@Nested
	@DisplayName("Modification")
	class Modification {

		@Test
		@DisplayName("un avoir ne peut pas être modifié")
		void avoir() {
			FactureEntity entity = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.AVOIR, true);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.updateFacture(9L, FactureRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("avoir");
		}

		@ParameterizedTest
		@CsvSource({"FACTUREE", "PAYEE", "ANNULEE"})
		@DisplayName("seules les factures BROUILLON ou PROFORMA sont modifiables")
		void statutsVerrouilles(FactureStatusEnum statut) {
			FactureEntity entity = facture(statut, FactureNatureEnum.FACTURE, true);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.updateFacture(9L, FactureRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("brouillon ou proforma");
		}

		@Test
		@DisplayName("la mise à jour d'une proforma remplace intégralement ses lignes")
		void remplacementDesLignes() {
			FactureEntity entity = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.FACTURE, true);
			LigneFactureEntity ancienne = LigneFactureEntity.builder().id(1L).build();
			FactureRequest request = FactureRequest.builder()
					.items(List.of(LigneFactureRequest.builder().designation("Nouvelle").build())).build();
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of(ancienne));
			when(ligneFactureMapper.toEntity(any())).thenReturn(new LigneFactureEntity());

			service.updateFacture(9L, request);

			verify(ligneFactureRepository).deleteAll(List.of(ancienne));
			verify(ligneFactureRepository).save(any(LigneFactureEntity.class));
		}

		@Test
		@DisplayName("une ligne appartenant à une autre facture est traitée comme introuvable")
		void ligneCroisee() {
			LigneFactureEntity ligne = LigneFactureEntity.builder()
					.id(5L).facture(FactureEntity.builder().id(2L).build()).build();
			when(factureRepository.existsById(9L)).thenReturn(true);
			when(ligneFactureRepository.findById(5L)).thenReturn(Optional.of(ligne));

			assertThatThrownBy(() -> service.getLigneById(9L, 5L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("n'appartient pas à la facture 9");
		}
	}

	@Nested
	@DisplayName("Avoir et remboursement")
	class AvoirRemboursement {

		@Test
		@DisplayName("un avoir client continue la séquence DA/01/79 et recopie les lignes")
		void avoirClient() {
			FactureEntity origine = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.FACTURE, true);
			LigneFactureEntity ligne = LigneFactureEntity.builder()
					.id(1L).designation("Location").montantHt(100_000L).qte(1L).build();
			when(factureRepository.findMaxNumProformaSuffix("DA/01/79/")).thenReturn(41);
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of(ligne));

			FactureEntity avoir = service.genererAvoir(origine);

			assertThat(avoir.getNature()).isEqualTo(FactureNatureEnum.AVOIR);
			assertThat(avoir.getStatut()).isEqualTo(FactureStatusEnum.FACTUREE);
			assertThat(avoir.getNumProforma()).isEqualTo("DA/01/79/42");
			assertThat(avoir.getFactureOrigine()).isSameAs(origine);
			assertThat(avoir.getMontantTtc()).isEqualTo(118_000L);
			assertThat(avoir.getObjet()).contains("AVOIR sur facture");
			verify(ligneFactureRepository).save(any(LigneFactureEntity.class));
		}

		@Test
		@DisplayName("un avoir fournisseur est préfixé AV- pour rester distinguable")
		void avoirFournisseur() {
			FactureEntity origine = facture(FactureStatusEnum.FACTUREE, FactureNatureEnum.FACTURE, false);
			origine.setNumFacture("F-EXTERNE-77");

			FactureEntity avoir = service.genererAvoir(origine);

			assertThat(avoir.getNumFacture()).isEqualTo("AV-F-EXTERNE-77");
		}

		@Test
		@DisplayName("le remboursement crée une ligne REMBOURSEMENT rattachée à la facture")
		void remboursement() {
			FactureEntity facture = facture(FactureStatusEnum.PAYEE, FactureNatureEnum.FACTURE, true);
			LigneCompteEntity ligne = new LigneCompteEntity();
			when(compteService.createLigneEntity(eq(3L), any(LigneCompteRequest.class), eq(LigneCompteOrigine.FACTURE))).thenReturn(ligne);
			when(ligneCompteRepository.save(ligne)).thenReturn(ligne);

			service.enregistrerRemboursement(facture, 3L);

			ArgumentCaptor<LigneCompteRequest> captor = ArgumentCaptor.forClass(LigneCompteRequest.class);
			verify(compteService).createLigneEntity(eq(3L), captor.capture(), eq(LigneCompteOrigine.FACTURE));
			assertThat(captor.getValue().getType()).isEqualTo(CompteLigneType.REMBOURSEMENT);
			assertThat(captor.getValue().getMontant()).isEqualTo(118_000L);
			assertThat(ligne.getFacture()).isSameAs(facture);
		}
	}

	@Nested
	@DisplayName("Resynchronisation depuis une mission")
	class Resynchronisation {

		@Test
		@DisplayName("les lignes sont remplacées et les montants recalculés avec la TVA")
		void recalcul() {
			FactureEntity facture = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.FACTURE, true);
			facture.setTva(18f);
			List<LigneFactureRequest> items = List.of(
					LigneFactureRequest.builder().designation("Location").montantHt(200_000L).build());
			when(ligneFactureMapper.toEntity(any())).thenReturn(new LigneFactureEntity());
			when(ligneFactureRepository.findByFactureId(9L)).thenReturn(List.of());

			service.replaceMissionFactureLines(facture, null, items);

			assertThat(facture.getMontantHt()).isEqualTo(200_000L);
			assertThat(facture.getMontantTtc()).isEqualTo(236_000L);
		}

		@Test
		@DisplayName("le partenaire est mis à jour s'il diffère")
		void changementPartenaire() {
			FactureEntity facture = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.FACTURE, true);
			facture.setPartenaire(partenaire(TypePartenaireEnum.ENTREPRISE));
			PartenaireEntity nouveau = PartenaireEntity.builder().id(6L).build();

			service.replaceMissionFactureLines(facture, nouveau, List.of());

			assertThat(facture.getPartenaire()).isSameAs(nouveau);
		}

		@Test
		@DisplayName("un avoir ne peut pas être resynchronisé")
		void avoirRefuse() {
			FactureEntity facture = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.AVOIR, true);

			assertThatThrownBy(() -> service.replaceMissionFactureLines(facture, null, List.of()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("avoir");
		}

		@Test
		@DisplayName("une facture verrouillée ne peut pas être resynchronisée")
		void statutVerrouille() {
			FactureEntity facture = facture(FactureStatusEnum.PAYEE, FactureNatureEnum.FACTURE, true);

			assertThatThrownBy(() -> service.replaceMissionFactureLines(facture, null, List.of()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("BROUILLON ou PROFORMA");
		}
	}

	@Nested
	@DisplayName("Facturation groupée fournisseur")
	class GroupeeFournisseur {

		private LivraisonFournisseurEntity bl(Long id, StatutBonLivraisonEnum statut,
				BonCommandeEntity bc, FactureEntity facture) {
			LivraisonFournisseurEntity entity = LivraisonFournisseurEntity.builder()
					.numero("BLF-" + id).statut(statut).bonCommande(bc).build();
			entity.setId(id);
			entity.setFacture(facture);
			return entity;
		}

		private BonCommandeEntity bc(PartenaireEntity partenaire, Float tva) {
			return BonCommandeEntity.builder().id(1L).numero("BC-1").partenaire(partenaire).tva(tva).build();
		}

		@Test
		@DisplayName("les montants sont calculés à partir des quantités livrées et remises")
		void calculDesMontants() {
			PartenaireEntity fournisseur = partenaire(TypePartenaireEnum.ENTREPRISE);
			BonCommandeEntity bc = bc(fournisseur, 18f);
			LivraisonFournisseurEntity bl = bl(1L, StatutBonLivraisonEnum.VALIDE, bc, null);
			LigneBonCommandeEntity ligneBc = LigneBonCommandeEntity.builder()
					.id(1L).reference("REF").designation("Pneu")
					.prixUnitaire(10_000L).remise(10f).build();
			EntreeProduitEntity entree = EntreeProduitEntity.builder()
					.id(1L).quantite(5L).ligneBonCommande(ligneBc).build();

			when(livraisonFournisseurRepository.findAllById(List.of(1L))).thenReturn(List.of(bl));
			when(factureRepository.findMaxNumProformaSuffix("DA/01/79/")).thenReturn(0);
			when(entreeProduitRepository.findByLivraisonFournisseurId(1L)).thenReturn(List.of(entree));

			service.genererFactureFournisseurGroupee(
					FactureFournisseurGroupeeRequest.builder().livraisonIds(List.of(1L)).build());

			ArgumentCaptor<FactureEntity> captor = ArgumentCaptor.forClass(FactureEntity.class);
			verify(factureRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
			FactureEntity facture = captor.getValue();
			assertThat(facture.getMontantHt()).isEqualTo(45_000L);
			assertThat(facture.getMontantTtc()).isEqualTo(53_100L);
			assertThat(facture.getStatut()).isEqualTo(FactureStatusEnum.FACTUREE);
			assertThat(facture.getFactureClient()).isFalse();
			assertThat(bl.getFacture()).isSameAs(facture);
		}

		@Test
		@DisplayName("un bon de livraison introuvable interrompt la facturation")
		void livraisonIntrouvable() {
			when(livraisonFournisseurRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of());

			assertThatThrownBy(() -> service.genererFactureFournisseurGroupee(
					FactureFournisseurGroupeeRequest.builder().livraisonIds(List.of(1L, 2L)).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("introuvables");
		}

		@Test
		@DisplayName("un bon non VALIDE est refusé")
		void livraisonNonValide() {
			BonCommandeEntity bc = bc(partenaire(TypePartenaireEnum.ENTREPRISE), 0f);
			LivraisonFournisseurEntity bl = bl(1L, StatutBonLivraisonEnum.CREE, bc, null);
			when(livraisonFournisseurRepository.findAllById(List.of(1L))).thenReturn(List.of(bl));

			assertThatThrownBy(() -> service.genererFactureFournisseurGroupee(
					FactureFournisseurGroupeeRequest.builder().livraisonIds(List.of(1L)).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'est pas VALIDE");
		}

		@Test
		@DisplayName("un bon déjà facturé est refusé")
		void livraisonDejaFacturee() {
			BonCommandeEntity bc = bc(partenaire(TypePartenaireEnum.ENTREPRISE), 0f);
			LivraisonFournisseurEntity bl = bl(1L, StatutBonLivraisonEnum.VALIDE, bc,
					FactureEntity.builder().id(2L).numFacture("F-1").build());
			when(livraisonFournisseurRepository.findAllById(List.of(1L))).thenReturn(List.of(bl));

			assertThatThrownBy(() -> service.genererFactureFournisseurGroupee(
					FactureFournisseurGroupeeRequest.builder().livraisonIds(List.of(1L)).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà rattaché");
		}

		@Test
		@DisplayName("des fournisseurs différents ne peuvent pas être facturés ensemble")
		void fournisseursDifferents() {
			BonCommandeEntity bc1 = bc(partenaire(TypePartenaireEnum.ENTREPRISE), 0f);
			BonCommandeEntity bc2 = BonCommandeEntity.builder().id(2L)
					.partenaire(PartenaireEntity.builder().id(6L).build()).tva(0f).build();
			when(livraisonFournisseurRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
					bl(1L, StatutBonLivraisonEnum.VALIDE, bc1, null),
					bl(2L, StatutBonLivraisonEnum.VALIDE, bc2, null)));

			assertThatThrownBy(() -> service.genererFactureFournisseurGroupee(
					FactureFournisseurGroupeeRequest.builder().livraisonIds(List.of(1L, 2L)).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("même fournisseur");
		}

		@Test
		@DisplayName("des TVA hétérogènes sont refusées")
		void tvaHeterogene() {
			PartenaireEntity fournisseur = partenaire(TypePartenaireEnum.ENTREPRISE);
			BonCommandeEntity bc1 = bc(fournisseur, 18f);
			BonCommandeEntity bc2 = BonCommandeEntity.builder().id(2L).partenaire(fournisseur).tva(0f).build();
			when(livraisonFournisseurRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
					bl(1L, StatutBonLivraisonEnum.VALIDE, bc1, null),
					bl(2L, StatutBonLivraisonEnum.VALIDE, bc2, null)));

			assertThatThrownBy(() -> service.genererFactureFournisseurGroupee(
					FactureFournisseurGroupeeRequest.builder().livraisonIds(List.of(1L, 2L)).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("TVA hétérogène");
		}

		@Test
		@DisplayName("un bon sans partenaire identifiable est refusé")
		void sansPartenaire() {
			LivraisonFournisseurEntity bl = bl(1L, StatutBonLivraisonEnum.VALIDE, null, null);
			when(livraisonFournisseurRepository.findAllById(List.of(1L))).thenReturn(List.of(bl));

			assertThatThrownBy(() -> service.genererFactureFournisseurGroupee(
					FactureFournisseurGroupeeRequest.builder().livraisonIds(List.of(1L)).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("sans partenaire identifiable");
		}
	}

	@Nested
	@DisplayName("Facturation groupée de missions")
	class GroupeeMissions {

		private MissionEntity mission(String code, PartenaireEntity client, TypeTarificationEnum type) {
			return MissionEntity.builder()
					.id(1L).codeMission(code).client(client).typeTarification(type)
					.vehicule(VehiculeEntity.builder().id(1L).immatriculation("AB-123-CD").build())
					.build();
		}

		private FactureMissionGroupeeRequest requete(Float tva, BigDecimal tarif, Long duree) {
			return FactureMissionGroupeeRequest.builder()
					.partenaireId(5L).tva(tva)
					.missions(List.of(FactureMissionItemRequest.builder()
							.missionId(1L).tarif(tarif).dureeLocation(duree).build()))
					.build();
		}

		@Test
		@DisplayName("le montant HT est la somme tarif × durée, TTC avec TVA")
		void montants() {
			PartenaireEntity client = partenaire(TypePartenaireEnum.ENTREPRISE);
			MissionEntity mission = mission("2026-001", client, TypeTarificationEnum.JOURNALIERE);
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.findAllById(List.of(1L))).thenReturn(List.of(mission));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of());
			when(factureRepository.findMaxNumProformaSuffix("DA/01/79/")).thenReturn(0);

			service.genererFactureMissionGroupee(requete(18f, BigDecimal.valueOf(30_000), 3L));

			ArgumentCaptor<FactureEntity> captor = ArgumentCaptor.forClass(FactureEntity.class);
			verify(factureRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
			FactureEntity facture = captor.getValue();
			assertThat(facture.getMontantHt()).isEqualTo(90_000L);
			assertThat(facture.getMontantTtc()).isEqualTo(106_200L);
			assertThat(facture.getType()).isEqualTo(FactureTypeEnum.MISSION);
			assertThat(facture.getStatut()).isEqualTo(FactureStatusEnum.PROFORMA);
		}

		@Test
		@DisplayName("un forfait UNIQUE est facturé en une seule quantité")
		void forfaitUnique() {
			PartenaireEntity client = partenaire(TypePartenaireEnum.ENTREPRISE);
			MissionEntity mission = mission("2026-001", client, TypeTarificationEnum.UNIQUE);
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.findAllById(List.of(1L))).thenReturn(List.of(mission));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of());
			when(factureRepository.findMaxNumProformaSuffix("DA/01/79/")).thenReturn(0);

			service.genererFactureMissionGroupee(requete(0f, BigDecimal.valueOf(150_000), 10L));

			ArgumentCaptor<LigneFactureEntity> captor = ArgumentCaptor.forClass(LigneFactureEntity.class);
			verify(ligneFactureRepository).save(captor.capture());
			assertThat(captor.getValue().getQte()).isEqualTo(1L);
			assertThat(captor.getValue().getMontantHt()).isEqualTo(150_000L);
			assertThat(captor.getValue().getExtraRef()).isEqualTo("2026-001");
			assertThat(captor.getValue().getDesignation()).contains("forfait");
		}

		@Test
		@DisplayName("une mission d'un autre client est refusée")
		void missionAutreClient() {
			PartenaireEntity client = partenaire(TypePartenaireEnum.ENTREPRISE);
			MissionEntity mission = mission("2026-001",
					PartenaireEntity.builder().id(6L).build(), TypeTarificationEnum.JOURNALIERE);
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.findAllById(List.of(1L))).thenReturn(List.of(mission));

			assertThatThrownBy(() -> service.genererFactureMissionGroupee(
					requete(0f, BigDecimal.TEN, 1L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'appartient pas au client");
		}

		@Test
		@DisplayName("une mission annulée ne peut pas être facturée")
		void missionAnnulee() {
			PartenaireEntity client = partenaire(TypePartenaireEnum.ENTREPRISE);
			MissionEntity mission = mission("2026-001", client, TypeTarificationEnum.JOURNALIERE);
			mission.setDhmsAnnulation(java.time.LocalDateTime.now());
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.findAllById(List.of(1L))).thenReturn(List.of(mission));

			assertThatThrownBy(() -> service.genererFactureMissionGroupee(
					requete(0f, BigDecimal.TEN, 1L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("annulée");
		}

		@Test
		@DisplayName("une mission déjà facturée est refusée")
		void missionDejaFacturee() {
			PartenaireEntity client = partenaire(TypePartenaireEnum.ENTREPRISE);
			MissionEntity mission = mission("2026-001", client, TypeTarificationEnum.JOURNALIERE);
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.findAllById(List.of(1L))).thenReturn(List.of(mission));
			when(factureRepository.findByLigneExtraRef("2026-001"))
					.thenReturn(List.of(FactureEntity.builder().id(2L).build()));

			assertThatThrownBy(() -> service.genererFactureMissionGroupee(
					requete(0f, BigDecimal.TEN, 1L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà rattachée à une facture");
		}

		@Test
		@DisplayName("un tarif nul ou négatif est refusé")
		void tarifInvalide() {
			PartenaireEntity client = partenaire(TypePartenaireEnum.ENTREPRISE);
			MissionEntity mission = mission("2026-001", client, TypeTarificationEnum.JOURNALIERE);
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.findAllById(List.of(1L))).thenReturn(List.of(mission));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of());
			when(factureRepository.findMaxNumProformaSuffix(anyString())).thenReturn(0);

			assertThatThrownBy(() -> service.genererFactureMissionGroupee(
					requete(0f, BigDecimal.ZERO, 1L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("tarif de la mission");
		}

		@Test
		@DisplayName("une durée nulle ou négative est refusée")
		void dureeInvalide() {
			PartenaireEntity client = partenaire(TypePartenaireEnum.ENTREPRISE);
			MissionEntity mission = mission("2026-001", client, TypeTarificationEnum.JOURNALIERE);
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.findAllById(List.of(1L))).thenReturn(List.of(mission));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of());
			when(factureRepository.findMaxNumProformaSuffix(anyString())).thenReturn(0);

			assertThatThrownBy(() -> service.genererFactureMissionGroupee(
					requete(0f, BigDecimal.TEN, 0L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("durée de location");
		}

		@Test
		@DisplayName("une liste de missions vide est refusée")
		void listeVide() {
			when(partenaireRepository.findById(5L))
					.thenReturn(Optional.of(partenaire(TypePartenaireEnum.ENTREPRISE)));

			assertThatThrownBy(() -> service.genererFactureMissionGroupee(
					FactureMissionGroupeeRequest.builder().partenaireId(5L).missions(List.of()).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Au moins une mission");
		}

		@Test
		@DisplayName("un client PARTICULIER donne une facture groupée de nature REÇU")
		void clientParticulier() {
			PartenaireEntity client = partenaire(TypePartenaireEnum.PARTICULIER);
			MissionEntity mission = mission("2026-001", client, TypeTarificationEnum.JOURNALIERE);
			when(partenaireRepository.findById(5L)).thenReturn(Optional.of(client));
			when(missionRepository.findAllById(List.of(1L))).thenReturn(List.of(mission));
			when(factureRepository.findByLigneExtraRef("2026-001")).thenReturn(List.of());
			when(factureRepository.findMaxNumProformaSuffix(anyString())).thenReturn(0);

			service.genererFactureMissionGroupee(requete(0f, BigDecimal.TEN, 1L));

			ArgumentCaptor<FactureEntity> captor = ArgumentCaptor.forClass(FactureEntity.class);
			verify(factureRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
			assertThat(captor.getAllValues().get(0).getNature()).isEqualTo(FactureNatureEnum.RECU);
		}
	}

	@Nested
	@DisplayName("Impression")
	class Impression {

		@Test
		@DisplayName("un avoir utilise le gabarit d'avoir")
		void gabaritAvoir() {
			FactureEntity entity = facture(FactureStatusEnum.FACTUREE, FactureNatureEnum.AVOIR, true);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));
			when(printService.generatePdf(anyString(), any())).thenReturn(new byte[]{1});

			service.generatePdf(9L);

			verify(printService).generatePdf(eq("pdf/factureAvoir"), any());
		}

		@Test
		@DisplayName("une facture de mission utilise le gabarit de location")
		void gabaritLocation() {
			FactureEntity entity = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.FACTURE, true);
			entity.setType(FactureTypeEnum.MISSION);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));
			when(printService.generatePdf(anyString(), any())).thenReturn(new byte[]{1});

			service.generatePdf(9L);

			verify(printService).generatePdf(eq("pdf/factureLocation"), any());
		}

		@Test
		@DisplayName("les autres factures utilisent le gabarit proforma")
		void gabaritProforma() {
			FactureEntity entity = facture(FactureStatusEnum.PROFORMA, FactureNatureEnum.FACTURE, true);
			entity.setType(FactureTypeEnum.PRODUIT);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));
			when(printService.generatePdf(anyString(), any())).thenReturn(new byte[]{1});

			service.generatePdf(9L);

			verify(printService).generatePdf(eq("pdf/factureProforma"), any());
		}

		@Test
		@DisplayName("la pièce de caisse est marquée RECETTE pour une facture client")
		void pieceDeCaisseRecette() {
			FactureEntity entity = facture(FactureStatusEnum.PAYEE, FactureNatureEnum.FACTURE, true);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));
			when(printService.generatePdf(anyString(), any())).thenReturn(new byte[]{1});

			service.generatePieceDeConsigne(9L);

			@SuppressWarnings("unchecked")
			var captor = (ArgumentCaptor<java.util.Map<String, Object>>)
					(ArgumentCaptor<?>) ArgumentCaptor.forClass(java.util.Map.class);
			verify(printService).generatePdf(eq("pdf/pieceDeConsigne"), captor.capture());
			assertThat(captor.getValue().get("isRecette")).isEqualTo(true);
		}

		@Test
		@DisplayName("la pièce de caisse est marquée DÉPENSE pour une facture fournisseur")
		void pieceDeCaisseDepense() {
			FactureEntity entity = facture(FactureStatusEnum.PAYEE, FactureNatureEnum.FACTURE, false);
			when(factureRepository.findById(9L)).thenReturn(Optional.of(entity));
			when(printService.generatePdf(anyString(), any())).thenReturn(new byte[]{1});

			service.generatePieceDeConsigne(9L);

			@SuppressWarnings("unchecked")
			var captor = (ArgumentCaptor<java.util.Map<String, Object>>)
					(ArgumentCaptor<?>) ArgumentCaptor.forClass(java.util.Map.class);
			verify(printService).generatePdf(eq("pdf/pieceDeConsigne"), captor.capture());
			assertThat(captor.getValue().get("isRecette")).isEqualTo(false);
		}

		@Test
		@DisplayName("imprimer une facture inconnue lève 404")
		void factureInconnue() {
			when(factureRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.generatePdf(99L))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("Recherche des factures d'une mission")
	class RechercheParMission {

		@Test
		@DisplayName("un code mission vide ne déclenche aucune requête")
		void codeVide() {
			assertThat(service.getFacturesByCodeMission("  ")).isEmpty();
			assertThat(service.getFacturesByCodeMission(null)).isEmpty();
			verify(factureRepository, never()).findByLigneExtraRef(anyString());
		}

		@Test
		@DisplayName("la facture historique liée par numProforma est ajoutée sans doublon")
		void factureHistorique() {
			FactureEntity parLigne = FactureEntity.builder().id(9L).build();
			FactureEntity historique = FactureEntity.builder().id(10L).build();
			when(factureRepository.findByLigneExtraRef("2026-001"))
					.thenReturn(new java.util.ArrayList<>(List.of(parLigne)));
			when(factureRepository.findByNumProforma("2026-001")).thenReturn(Optional.of(historique));

			assertThat(service.getFacturesByCodeMission("2026-001")).hasSize(2);
		}

		@Test
		@DisplayName("une facture déjà présente n'est pas dupliquée")
		void pasDeDoublon() {
			FactureEntity facture = FactureEntity.builder().id(9L).build();
			when(factureRepository.findByLigneExtraRef("2026-001"))
					.thenReturn(new java.util.ArrayList<>(List.of(facture)));
			when(factureRepository.findByNumProforma("2026-001")).thenReturn(Optional.of(facture));

			assertThat(service.getFacturesByCodeMission("2026-001")).hasSize(1);
		}
	}

	@Nested
	@DisplayName("Recherche paginée multi-critères")
	class Recherche {

		private final Pageable pageable = PageRequest.of(0, 20);

		@BeforeEach
		void videParDefaut() {
			when(factureRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(), any()))
					.thenReturn(Page.empty());
		}

		@Test
		@DisplayName("les factures clients forcent factureClient à vrai, les fournisseurs à faux")
		void sensDeLaFacture() {
			service.getFacturesClients(null, null, null, null, null, null, pageable);
			verify(factureRepository).findFiltered(null, true, null, null, null, null, null, pageable);

			service.getFacturesFournisseurs(null, null, null, null, null, null, pageable);
			verify(factureRepository).findFiltered(null, false, null, null, null, null, null, pageable);
		}

		@Test
		@DisplayName("tous les critères de recherche sont transmis au dépôt")
		void criteresTransmis() {
			service.getFacturesClients("acme", 7L, "DA/01", "2026-001",
					LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), pageable);

			verify(factureRepository).findFiltered("acme", true, 7L, "DA/01", "2026-001",
					LocalDateTime.of(2026, 3, 1, 0, 0),
					LocalDateTime.of(2026, 4, 1, 0, 0),
					pageable);
		}

		@Test
		@DisplayName("les critères textuels vides ou en blanc sont ramenés à null et n'appliquent aucun filtre")
		void criteresVidesNormalises() {
			service.getFacturesClients("   ", null, "", "  ", null, null, pageable);

			verify(factureRepository).findFiltered(null, true, null, null, null, null, null, pageable);
		}

		@Test
		@DisplayName("les critères textuels sont débarrassés de leurs espaces superflus")
		void criteresTrimmes() {
			service.getFacturesFournisseurs("  acme  ", null, "  DA/01  ", null, null, null, pageable);

			verify(factureRepository).findFiltered("acme", false, null, "DA/01", null, null, null, pageable);
		}

		@Test
		@DisplayName("une seule borne de date suffit : l'autre reste nulle")
		void borneUnique() {
			service.getFacturesClients(null, null, null, null, LocalDate.of(2026, 3, 1), null, pageable);

			verify(factureRepository).findFiltered(null, true, null, null, null,
					LocalDateTime.of(2026, 3, 1, 0, 0), null, pageable);
		}
	}
}
