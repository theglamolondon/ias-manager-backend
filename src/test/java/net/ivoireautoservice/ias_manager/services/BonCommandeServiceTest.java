package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.config.MoneyUtils;
import net.ivoireautoservice.ias_manager.dto.core.BonCommande;
import net.ivoireautoservice.ias_manager.dto.core.Facture;
import net.ivoireautoservice.ias_manager.dto.core.LigneBonCommande;
import net.ivoireautoservice.ias_manager.dto.request.BonCommandeRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneBonCommandeRequest;
import net.ivoireautoservice.ias_manager.entity.BonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.FactureEntity;
import net.ivoireautoservice.ias_manager.entity.LigneBonCommandeEntity;
import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.BonCommandeMapper;
import net.ivoireautoservice.ias_manager.mapper.FactureMapper;
import net.ivoireautoservice.ias_manager.mapper.LigneBonCommandeMapper;
import net.ivoireautoservice.ias_manager.mapper.LivraisonFournisseurMapper;
import net.ivoireautoservice.ias_manager.repository.BonCommandeRepository;
import net.ivoireautoservice.ias_manager.repository.LigneBonCommandeRepository;
import net.ivoireautoservice.ias_manager.repository.LivraisonFournisseurRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import net.ivoireautoservice.ias_manager.repository.ProduitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BonCommandeService — cycle de vie et impression des bons de commande")
class BonCommandeServiceTest {

	@Mock private BonCommandeRepository bonCommandeRepository;
	@Mock private LigneBonCommandeRepository ligneBonCommandeRepository;
	@Mock private PartenaireRepository partenaireRepository;
	@Mock private ProduitRepository produitRepository;
	@Mock private BonCommandeMapper bonCommandeMapper;
	@Mock private LigneBonCommandeMapper ligneBonCommandeMapper;
	@Mock private LivraisonFournisseurRepository livraisonFournisseurRepository;
	@Mock private LivraisonFournisseurMapper livraisonFournisseurMapper;
	@Mock private FactureMapper factureMapper;
	@Mock private PrintService printService;
	@Mock private MoneyUtils moneyUtils;
	@Mock private SecurityService securityService;

	@InjectMocks
	private BonCommandeService service;

	private static PartenaireEntity fournisseur(boolean estFournisseur) {
		return PartenaireEntity.builder().id(1L).raisonSociale("Total CI")
				.isFournisseur(estFournisseur).build();
	}

	private static BonCommandeEntity bc(BonCommandeStatusEnum statut) {
		return BonCommandeEntity.builder().id(1L).numero("BC-2026-001").statut(statut)
				.partenaire(fournisseur(true)).montantHt(100_000L).montantTtc(118_000L).build();
	}

	private void stubDto(BonCommandeEntity entity) {
		when(bonCommandeMapper.toDto(entity)).thenReturn(BonCommande.builder()
				.montantHt(entity.getMontantHt()).montantTtc(entity.getMontantTtc()).build());
		when(ligneBonCommandeRepository.findByBonCommandeId(entity.getId())).thenReturn(List.of());
		when(ligneBonCommandeMapper.toDtoList(any())).thenReturn(List.of());
	}

	@Nested
	@DisplayName("Création")
	class Creation {

		@Test
		@DisplayName("le partenaire est obligatoire")
		void partenaireObligatoire() {
			assertThatThrownBy(() -> service.create(BonCommandeRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("fournisseur (partenaireId) est obligatoire");
		}

		@Test
		@DisplayName("un partenaire qui n'est pas fournisseur est refusé")
		void partenaireNonFournisseur() {
			when(partenaireRepository.findById(1L)).thenReturn(Optional.of(fournisseur(false)));

			assertThatThrownBy(() -> service.create(BonCommandeRequest.builder().partenaireId(1L).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'est pas un fournisseur");
			verify(bonCommandeRepository, never()).save(any());
		}

		@Test
		@DisplayName("le bon est créé au statut CREE, numéroté BC-{année}-{séquence}")
		void numerotation() {
			BonCommandeEntity entity = new BonCommandeEntity();
			BonCommandeRequest request = BonCommandeRequest.builder().partenaireId(1L).build();
			when(partenaireRepository.findById(1L)).thenReturn(Optional.of(fournisseur(true)));
			when(bonCommandeMapper.toEntity(request)).thenReturn(entity);
			when(bonCommandeRepository.findMaxNumeroSuffix(anyString())).thenReturn(12);
			when(bonCommandeRepository.save(entity)).thenReturn(entity);
			when(bonCommandeMapper.toDto(entity)).thenReturn(BonCommande.builder().build());
			when(ligneBonCommandeMapper.toDtoList(any())).thenReturn(List.of());

			service.create(request);

			int annee = LocalDate.now().getYear();
			assertThat(entity.getStatut()).isEqualTo(BonCommandeStatusEnum.CREE);
			assertThat(entity.getNumero()).isEqualTo("BC-" + annee + "-013");
			assertThat(entity.getDateCommande()).isEqualTo(LocalDate.now());
		}

		@Test
		@DisplayName("la numérotation démarre à 001 quand aucun bon n'existe pour l'année")
		void premiereNumerotation() {
			BonCommandeEntity entity = new BonCommandeEntity();
			BonCommandeRequest request = BonCommandeRequest.builder().partenaireId(1L).build();
			when(partenaireRepository.findById(1L)).thenReturn(Optional.of(fournisseur(true)));
			when(bonCommandeMapper.toEntity(request)).thenReturn(entity);
			when(bonCommandeRepository.findMaxNumeroSuffix(anyString())).thenReturn(null);
			when(bonCommandeRepository.save(entity)).thenReturn(entity);
			when(bonCommandeMapper.toDto(entity)).thenReturn(BonCommande.builder().build());
			when(ligneBonCommandeMapper.toDtoList(any())).thenReturn(List.of());

			service.create(request);

			assertThat(entity.getNumero()).endsWith("-001");
		}

		@Test
		@DisplayName("les lignes fournies sont enregistrées avec une quantité livrée à zéro")
		void lignesInitialisees() {
			BonCommandeEntity entity = BonCommandeEntity.builder().id(1L).build();
			LigneBonCommandeEntity ligne = new LigneBonCommandeEntity();
			LigneBonCommandeRequest item = LigneBonCommandeRequest.builder()
					.reference("REF-1").qte(10L).produitId(3L).build();
			BonCommandeRequest request = BonCommandeRequest.builder()
					.partenaireId(1L).items(List.of(item)).build();
			when(partenaireRepository.findById(1L)).thenReturn(Optional.of(fournisseur(true)));
			when(bonCommandeMapper.toEntity(request)).thenReturn(entity);
			when(bonCommandeRepository.save(entity)).thenReturn(entity);
			when(ligneBonCommandeMapper.toEntity(item)).thenReturn(ligne);
			when(produitRepository.findById(3L)).thenReturn(Optional.of(ProduitEntity.builder().id(3L).build()));
			when(ligneBonCommandeRepository.save(ligne)).thenReturn(ligne);
			when(bonCommandeMapper.toDto(entity)).thenReturn(BonCommande.builder().build());
			when(ligneBonCommandeMapper.toDtoList(any())).thenReturn(List.of());

			service.create(request);

			assertThat(ligne.getQteLivree()).isZero();
			assertThat(ligne.getBonCommande()).isSameAs(entity);
			assertThat(ligne.getProduit()).isNotNull();
		}

		@Test
		@DisplayName("l'émetteur connecté est enregistré comme créateur")
		void createdBy() {
			Utilisateur utilisateur = Utilisateur.builder().id(7L).build();
			BonCommandeEntity entity = new BonCommandeEntity();
			BonCommandeRequest request = BonCommandeRequest.builder().partenaireId(1L).build();
			when(partenaireRepository.findById(1L)).thenReturn(Optional.of(fournisseur(true)));
			when(bonCommandeMapper.toEntity(request)).thenReturn(entity);
			when(securityService.getUtilisateurConnecteOrNull()).thenReturn(utilisateur);
			when(bonCommandeRepository.save(entity)).thenReturn(entity);
			when(bonCommandeMapper.toDto(entity)).thenReturn(BonCommande.builder().build());
			when(ligneBonCommandeMapper.toDtoList(any())).thenReturn(List.of());

			service.create(request);

			assertThat(entity.getCreatedBy()).isSameAs(utilisateur);
		}
	}

	@Nested
	@DisplayName("Transitions de statut")
	class Transitions {

		@Test
		@DisplayName("valider un bon CREE avec au moins une ligne le passe VALIDE")
		void validation() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.CREE);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(ligneBonCommandeRepository.findByBonCommandeId(1L))
					.thenReturn(List.of(new LigneBonCommandeEntity()));
			when(bonCommandeRepository.save(entity)).thenReturn(entity);
			when(bonCommandeMapper.toDto(entity)).thenReturn(BonCommande.builder().build());
			when(ligneBonCommandeMapper.toDtoList(any())).thenReturn(List.of());

			service.valider(1L);

			assertThat(entity.getStatut()).isEqualTo(BonCommandeStatusEnum.VALIDE);
		}

		@Test
		@DisplayName("un bon sans ligne ne peut pas être validé")
		void validationSansLigne() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.CREE);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(ligneBonCommandeRepository.findByBonCommandeId(1L)).thenReturn(List.of());

			assertThatThrownBy(() -> service.valider(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("sans ligne");
		}

		@ParameterizedTest
		@EnumSource(value = BonCommandeStatusEnum.class, names = {"CREE"}, mode = EnumSource.Mode.EXCLUDE)
		@DisplayName("seul un bon CREE peut être validé")
		void validationDepuisAutreStatut(BonCommandeStatusEnum statut) {
			BonCommandeEntity entity = bc(statut);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.valider(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("statut CREE");
		}

		@Test
		@DisplayName("annuler un bon VALIDE le passe ANNULE")
		void annulation() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.VALIDE);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(bonCommandeRepository.save(entity)).thenReturn(entity);
			when(bonCommandeMapper.toDto(entity)).thenReturn(BonCommande.builder().build());
			when(ligneBonCommandeMapper.toDtoList(any())).thenReturn(List.of());

			service.annuler(1L);

			assertThat(entity.getStatut()).isEqualTo(BonCommandeStatusEnum.ANNULE);
		}

		@ParameterizedTest
		@EnumSource(value = BonCommandeStatusEnum.class,
				names = {"LIVRE", "PARTIELLEMENT_LIVRE", "ANNULE"})
		@DisplayName("un bon livré, partiellement livré ou déjà annulé ne peut pas être annulé")
		void annulationRefusee(BonCommandeStatusEnum statut) {
			BonCommandeEntity entity = bc(statut);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> service.annuler(1L))
					.isInstanceOf(BadRequestException.class);
			assertThat(entity.getStatut()).isEqualTo(statut);
		}
	}

	@Nested
	@DisplayName("Modification et suppression")
	class ModificationSuppression {

		@ParameterizedTest
		@EnumSource(value = BonCommandeStatusEnum.class, names = {"CREE"}, mode = EnumSource.Mode.EXCLUDE)
		@DisplayName("seul un bon CREE est modifiable")
		void updateSeulementSiCree(BonCommandeStatusEnum statut) {
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc(statut)));

			assertThatThrownBy(() -> service.update(1L, BonCommandeRequest.builder().build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("statut CREE");
		}

		@Test
		@DisplayName("la mise à jour refuse un partenaire non fournisseur")
		void updatePartenaireNonFournisseur() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.CREE);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(partenaireRepository.findById(2L)).thenReturn(Optional.of(
					PartenaireEntity.builder().id(2L).isFournisseur(false).build()));

			assertThatThrownBy(() -> service.update(1L,
					BonCommandeRequest.builder().partenaireId(2L).build()))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'est pas un fournisseur");
		}

		@ParameterizedTest
		@EnumSource(value = BonCommandeStatusEnum.class, names = {"LIVRE", "PARTIELLEMENT_LIVRE"})
		@DisplayName("un bon ayant fait l'objet d'une livraison ne peut pas être supprimé")
		void suppressionRefusee(BonCommandeStatusEnum statut) {
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc(statut)));

			assertThatThrownBy(() -> service.delete(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("livraison");
			verify(bonCommandeRepository, never()).delete(any());
		}

		@Test
		@DisplayName("supprimer un bon CREE efface aussi ses lignes")
		void suppression() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.CREE);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));

			service.delete(1L);

			verify(ligneBonCommandeRepository).deleteByBonCommandeId(1L);
			verify(bonCommandeRepository).delete(entity);
		}
	}

	@Nested
	@DisplayName("Lignes du bon de commande")
	class Lignes {

		@ParameterizedTest
		@EnumSource(value = BonCommandeStatusEnum.class, names = {"CREE"}, mode = EnumSource.Mode.EXCLUDE)
		@DisplayName("les lignes ne sont modifiables qu'au statut CREE")
		void lignesVerrouillees(BonCommandeStatusEnum statut) {
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc(statut)));

			assertThatThrownBy(() -> service.createLigne(1L, LigneBonCommandeRequest.builder().build()))
					.isInstanceOf(BadRequestException.class);
			assertThatThrownBy(() -> service.deleteLigne(1L, 5L))
					.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("une ligne créée démarre avec une quantité livrée nulle")
		void creationLigne() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.CREE);
			LigneBonCommandeEntity ligne = new LigneBonCommandeEntity();
			LigneBonCommandeRequest request = LigneBonCommandeRequest.builder().qte(5L).build();
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(ligneBonCommandeMapper.toEntity(request)).thenReturn(ligne);
			when(ligneBonCommandeRepository.save(ligne)).thenReturn(ligne);
			when(ligneBonCommandeMapper.toDto(ligne)).thenReturn(LigneBonCommande.builder().build());

			service.createLigne(1L, request);

			assertThat(ligne.getQteLivree()).isZero();
			assertThat(ligne.getBonCommande()).isSameAs(entity);
		}

		@Test
		@DisplayName("modifier une ligne appartenant à un autre bon est refusé")
		void ligneCroisee() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.CREE);
			LigneBonCommandeEntity ligne = LigneBonCommandeEntity.builder()
					.id(5L).bonCommande(BonCommandeEntity.builder().id(2L).build()).build();
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(ligneBonCommandeRepository.findById(5L)).thenReturn(Optional.of(ligne));

			assertThatThrownBy(() -> service.updateLigne(1L, 5L, LigneBonCommandeRequest.builder().build()))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("n'appartient pas au bon de commande 1");
		}

		@Test
		@DisplayName("lister les lignes d'un bon inconnu lève 404")
		void lignesBonInconnu() {
			when(bonCommandeRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.getLignes(99L))
					.isInstanceOf(ResourceNotFoundException.class);
		}
	}

	@Nested
	@DisplayName("Impression PDF")
	class Impression {

		@Test
		@DisplayName("un bon au statut CREE ne peut pas être imprimé")
		void bonNonValide() {
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc(BonCommandeStatusEnum.CREE)));

			assertThatThrownBy(() -> service.generatePdf(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("validé avant d'être imprimé");
		}

		@Test
		@DisplayName("un bon annulé ne peut pas être imprimé")
		void bonAnnule() {
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(bc(BonCommandeStatusEnum.ANNULE)));

			assertThatThrownBy(() -> service.generatePdf(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("annulé");
		}

		@Test
		@DisplayName("un bon validé produit un PDF avec TVA calculée, montant en lettres et signature")
		void bonValide() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.VALIDE);
			stubDto(entity);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(moneyUtils.montantEnLettre(118_000)).thenReturn("Cent dix-huit mille Francs CFA");
			when(securityService.getUtilisateurConnecte()).thenReturn(
					Utilisateur.builder().id(7L).nom("Kouassi").prenom("Yao").telephone("0700000000").build());
			when(printService.generatePdf(anyString(), anyMap())).thenReturn(new byte[]{1, 2});

			byte[] pdf = service.generatePdf(1L);

			assertThat(pdf).isNotEmpty();
			@SuppressWarnings("unchecked")
			var captor = (org.mockito.ArgumentCaptor<Map<String, Object>>)
					(org.mockito.ArgumentCaptor<?>) org.mockito.ArgumentCaptor.forClass(Map.class);
			verify(printService).generatePdf(org.mockito.ArgumentMatchers.eq("pdf/BonDeCommande"), captor.capture());
			Map<String, Object> data = captor.getValue();
			assertThat(data.get("montantTva")).isEqualTo(18_000L);
			assertThat(data.get("montantEnLettres")).isEqualTo("Cent dix-huit mille Francs CFA");
			assertThat(data.get("emetteur")).isEqualTo("Yao Kouassi");
			assertThat(data.get("contactEmetteur")).isEqualTo("0700000000");
			assertThat(data).containsKey("signatureUrl");
		}

		@Test
		@DisplayName("l'absence d'utilisateur connecté laisse les champs émetteur vides sans échouer")
		void sansUtilisateurConnecte() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.VALIDE);
			stubDto(entity);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(securityService.getUtilisateurConnecte())
					.thenThrow(new IllegalStateException("Aucun utilisateur connecté"));
			when(printService.generatePdf(anyString(), anyMap())).thenReturn(new byte[]{1});

			assertThat(service.generatePdf(1L)).isNotEmpty();
		}
	}

	@Nested
	@DisplayName("Consultation détaillée")
	class ConsultationDetaillee {

		@Test
		@DisplayName("le détail expose les livraisons et les factures distinctes qui en découlent")
		void detailAvecLivraisonsEtFactures() {
			BonCommandeEntity entity = bc(BonCommandeStatusEnum.PARTIELLEMENT_LIVRE);
			FactureEntity facture = FactureEntity.builder().id(9L).build();
			LivraisonFournisseurEntity bl1 = LivraisonFournisseurEntity.builder().facture(facture).build();
			LivraisonFournisseurEntity bl2 = LivraisonFournisseurEntity.builder().facture(facture).build();
			bl1.setId(1L);
			bl2.setId(2L);
			stubDto(entity);
			when(bonCommandeRepository.findById(1L)).thenReturn(Optional.of(entity));
			when(livraisonFournisseurRepository.findByBonCommandeId(1L)).thenReturn(List.of(bl1, bl2));
			when(livraisonFournisseurMapper.toSummary(any())).thenReturn(null);
			when(factureMapper.toDto(facture)).thenReturn(Facture.builder().id(9L).build());

			BonCommande dto = service.getById(1L);

			assertThat(dto.getLivraisons()).hasSize(2);
			assertThat(dto.getFactures()).hasSize(1);
		}

		@Test
		@DisplayName("getByNumero lève 404 sur un numéro inconnu")
		void parNumeroInconnu() {
			when(bonCommandeRepository.findByNumero("BC-404")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getByNumero("BC-404"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("BC-404");
		}
	}
}
