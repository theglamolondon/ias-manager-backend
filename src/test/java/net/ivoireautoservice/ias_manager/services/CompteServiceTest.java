package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.dto.core.Compte;
import net.ivoireautoservice.ias_manager.dto.core.LigneCompte;
import net.ivoireautoservice.ias_manager.dto.request.CompteRequest;
import net.ivoireautoservice.ias_manager.dto.request.CompteUtilisateurRequest;
import net.ivoireautoservice.ias_manager.dto.request.LigneCompteRequest;
import net.ivoireautoservice.ias_manager.entity.CompteEntity;
import net.ivoireautoservice.ias_manager.entity.CompteUtilisateurEntity;
import net.ivoireautoservice.ias_manager.entity.LigneCompteEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.CompteMapper;
import net.ivoireautoservice.ias_manager.mapper.CompteUtilisateurMapper;
import net.ivoireautoservice.ias_manager.mapper.LigneCompteMapper;
import net.ivoireautoservice.ias_manager.repository.CompteRepository;
import net.ivoireautoservice.ias_manager.repository.CompteUtilisateurRepository;
import net.ivoireautoservice.ias_manager.repository.LigneCompteRepository;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompteService — mouvements de trésorerie et habilitations par compte")
class CompteServiceTest {

	@Mock
	private CompteRepository compteRepository;

	@Mock
	private CompteUtilisateurRepository compteUtilisateurRepository;

	@Mock
	private LigneCompteRepository ligneCompteRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private SecurityService securityService;

	@Mock
	private MediaService mediaService;

	@Mock
	private CompteMapper compteMapper;

	@Mock
	private CompteUtilisateurMapper compteUtilisateurMapper;

	@Mock
	private LigneCompteMapper ligneCompteMapper;

	@InjectMocks
	private CompteService service;

	private Utilisateur utilisateur;

	@BeforeEach
	void setUp() {
		utilisateur = Utilisateur.builder().id(7L).email("agent@ias.ci").build();
	}

	private static CompteEntity compte(long balance, boolean canAppro, boolean canBeNegative) {
		return CompteEntity.builder()
				.id(1L).intitule("Caisse principale").numero("C-001")
				.balance(balance).canAppro(canAppro).canBeNegative(canBeNegative)
				.build();
	}

	private CompteUtilisateurEntity habilitation(CompteEntity compte, boolean canAppro, boolean canSettle) {
		return CompteUtilisateurEntity.builder()
				.id(1L).compte(compte).utilisateur(utilisateur)
				.canAppro(canAppro).canSettle(canSettle).build();
	}

	private void stubHabilitation(CompteEntity compte, CompteUtilisateurEntity habilitation) {
		when(compteRepository.findById(1L)).thenReturn(Optional.of(compte));
		when(securityService.getUtilisateurConnecte()).thenReturn(utilisateur);
		when(compteUtilisateurRepository.findByCompteIdAndUtilisateurId(1L, 7L))
				.thenReturn(Optional.ofNullable(habilitation));
	}

	private static LigneCompteRequest ligne(CompteLigneType type, long montant) {
		return LigneCompteRequest.builder().type(type).montant(montant).objet("Test").build();
	}

	@Nested
	@DisplayName("Écriture d'une ligne : effet sur la balance")
	class EffetBalance {

		@Test
		@DisplayName("une DEPENSE débite le compte")
		void depense() {
			CompteEntity compte = compte(100_000L, false, false);
			stubHabilitation(compte, habilitation(compte, false, false));
			when(ligneCompteRepository.save(any(LigneCompteEntity.class))).thenAnswer(i -> i.getArgument(0));

			LigneCompteEntity resultat = service.createLigneEntity(1L, ligne(CompteLigneType.DEPENSE, 30_000L));

			assertThat(compte.getBalance()).isEqualTo(70_000L);
			assertThat(resultat.getBalanceAvant()).isEqualTo(100_000L);
			assertThat(resultat.getUtilisateur()).isSameAs(utilisateur);
			assertThat(resultat.getDhmsOperation()).isNotNull();
		}

		@Test
		@DisplayName("un REMBOURSEMENT débite également le compte")
		void remboursement() {
			CompteEntity compte = compte(100_000L, false, false);
			stubHabilitation(compte, habilitation(compte, false, false));
			when(ligneCompteRepository.save(any(LigneCompteEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.createLigneEntity(1L, ligne(CompteLigneType.REMBOURSEMENT, 25_000L));

			assertThat(compte.getBalance()).isEqualTo(75_000L);
		}

		@Test
		@DisplayName("un APPROVISIONNEMENT crédite le compte")
		void approvisionnement() {
			CompteEntity compte = compte(100_000L, true, false);
			stubHabilitation(compte, habilitation(compte, true, false));
			when(ligneCompteRepository.save(any(LigneCompteEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.createLigneEntity(1L, ligne(CompteLigneType.APPROVISIONNEMENT, 50_000L));

			assertThat(compte.getBalance()).isEqualTo(150_000L);
		}

		@Test
		@DisplayName("le compte modifié est bien persisté")
		void persistanceDuCompte() {
			CompteEntity compte = compte(100_000L, false, false);
			stubHabilitation(compte, habilitation(compte, false, false));
			when(ligneCompteRepository.save(any(LigneCompteEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.createLigneEntity(1L, ligne(CompteLigneType.DEPENSE, 10_000L));

			verify(compteRepository).save(compte);
		}
	}

	@Nested
	@DisplayName("Garde-fous sur les mouvements")
	class GardeFous {

		@Test
		@DisplayName("un utilisateur non habilité sur le compte est refusé")
		void utilisateurNonHabilite() {
			CompteEntity compte = compte(100_000L, false, false);
			stubHabilitation(compte, null);

			assertThatThrownBy(() -> service.createLigneEntity(1L, ligne(CompteLigneType.DEPENSE, 1_000L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("pas autorisé à effectuer des mouvements");
			verify(ligneCompteRepository, never()).save(any());
		}

		@Test
		@DisplayName("un compte n'autorisant pas l'approvisionnement refuse l'opération")
		void compteSansAppro() {
			CompteEntity compte = compte(100_000L, false, false);
			stubHabilitation(compte, habilitation(compte, true, false));

			assertThatThrownBy(() -> service.createLigneEntity(1L,
					ligne(CompteLigneType.APPROVISIONNEMENT, 1_000L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("n'autorise pas l'approvisionnement");
		}

		@Test
		@DisplayName("un utilisateur sans droit d'approvisionnement est refusé même sur un compte ouvert")
		void utilisateurSansAppro() {
			CompteEntity compte = compte(100_000L, true, false);
			stubHabilitation(compte, habilitation(compte, false, false));

			assertThatThrownBy(() -> service.createLigneEntity(1L,
					ligne(CompteLigneType.APPROVISIONNEMENT, 1_000L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("pas autorisé à approvisionner");
		}

		@Test
		@DisplayName("une dépense rendant la balance négative est refusée par défaut")
		void soldeInsuffisant() {
			CompteEntity compte = compte(10_000L, false, false);
			stubHabilitation(compte, habilitation(compte, false, false));

			assertThatThrownBy(() -> service.createLigneEntity(1L, ligne(CompteLigneType.DEPENSE, 15_000L)))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("Solde insuffisant");
			assertThat(compte.getBalance()).isEqualTo(10_000L);
			verify(compteRepository, never()).save(any());
		}

		@Test
		@DisplayName("un compte autorisant le découvert accepte une balance négative")
		void decouvertAutorise() {
			CompteEntity compte = compte(10_000L, false, true);
			stubHabilitation(compte, habilitation(compte, false, false));
			when(ligneCompteRepository.save(any(LigneCompteEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.createLigneEntity(1L, ligne(CompteLigneType.DEPENSE, 15_000L));

			assertThat(compte.getBalance()).isEqualTo(-5_000L);
		}

		@Test
		@DisplayName("une dépense ramenant la balance exactement à zéro est acceptée")
		void balanceExactementZero() {
			CompteEntity compte = compte(10_000L, false, false);
			stubHabilitation(compte, habilitation(compte, false, false));
			when(ligneCompteRepository.save(any(LigneCompteEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.createLigneEntity(1L, ligne(CompteLigneType.DEPENSE, 10_000L));

			assertThat(compte.getBalance()).isZero();
		}

		@Test
		@DisplayName("un compte inconnu lève 404")
		void compteInconnu() {
			when(compteRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createLigneEntity(99L, ligne(CompteLigneType.DEPENSE, 1L)))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Compte avec l'id 99");
		}
	}

	@Nested
	@DisplayName("Solde de compte")
	class Solde {

		@Test
		@DisplayName("solder remet la balance à zéro et enregistre le montant absolu")
		void solderCompteCrediteur() {
			CompteEntity compte = compte(45_000L, false, false);
			stubHabilitation(compte, habilitation(compte, false, true));
			when(ligneCompteRepository.save(any(LigneCompteEntity.class))).thenAnswer(i -> i.getArgument(0));
			when(ligneCompteMapper.toDto(any(LigneCompteEntity.class))).thenReturn(LigneCompte.builder().build());

			service.solderCompte(1L);

			assertThat(compte.getBalance()).isZero();
			var captor = org.mockito.ArgumentCaptor.forClass(LigneCompteEntity.class);
			verify(ligneCompteRepository).save(captor.capture());
			assertThat(captor.getValue().getType()).isEqualTo(CompteLigneType.SOLDE);
			assertThat(captor.getValue().getMontant()).isEqualTo(45_000L);
			assertThat(captor.getValue().getBalanceAvant()).isEqualTo(45_000L);
			assertThat(captor.getValue().getObjet()).isEqualTo("SOLDE DU COMPTE");
		}

		@Test
		@DisplayName("solder un compte débiteur enregistre la valeur absolue du découvert")
		void solderCompteDebiteur() {
			CompteEntity compte = compte(-20_000L, false, true);
			stubHabilitation(compte, habilitation(compte, false, true));
			when(ligneCompteRepository.save(any(LigneCompteEntity.class))).thenAnswer(i -> i.getArgument(0));
			when(ligneCompteMapper.toDto(any(LigneCompteEntity.class))).thenReturn(LigneCompte.builder().build());

			service.solderCompte(1L);

			var captor = org.mockito.ArgumentCaptor.forClass(LigneCompteEntity.class);
			verify(ligneCompteRepository).save(captor.capture());
			assertThat(captor.getValue().getMontant()).isEqualTo(20_000L);
			assertThat(compte.getBalance()).isZero();
		}

		@Test
		@DisplayName("solder exige le droit dédié canSettle")
		void sansDroitDeSolder() {
			CompteEntity compte = compte(45_000L, false, false);
			stubHabilitation(compte, habilitation(compte, true, false));

			assertThatThrownBy(() -> service.solderCompte(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("pas autorisé à solder");
		}

		@Test
		@DisplayName("un compte déjà soldé ne peut pas l'être à nouveau")
		void dejaSolde() {
			CompteEntity compte = compte(0L, false, false);
			stubHabilitation(compte, habilitation(compte, false, true));

			assertThatThrownBy(() -> service.solderCompte(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("déjà soldé");
		}

		@Test
		@DisplayName("un utilisateur non habilité ne peut pas solder")
		void nonHabilite() {
			CompteEntity compte = compte(45_000L, false, false);
			stubHabilitation(compte, null);

			assertThatThrownBy(() -> service.solderCompte(1L))
					.isInstanceOf(BadRequestException.class)
					.hasMessageContaining("pas autorisé à effectuer des mouvements");
		}
	}

	@Nested
	@DisplayName("Habilitations des utilisateurs sur un compte")
	class Habilitations {

		@Test
		@DisplayName("canAppro utilisateur est forcé à false si le compte n'autorise pas l'approvisionnement")
		void canApproForceAFalse() {
			CompteEntity compte = compte(0L, false, false);
			CompteRequest request = CompteRequest.builder()
					.intitule("Caisse").numero("C-001").canAppro(false)
					.utilisateurs(List.of(CompteUtilisateurRequest.builder()
							.utilisateurId(7L).canAppro(true).canSettle(true).build()))
					.build();
			when(compteMapper.toEntity(request)).thenReturn(compte);
			when(userRepository.findById(7L)).thenReturn(Optional.of(utilisateur));
			when(compteRepository.save(compte)).thenReturn(compte);
			when(compteMapper.toDto(compte)).thenReturn(Compte.builder().build());

			service.createCompte(request, null);

			assertThat(compte.getUtilisateurs()).hasSize(1);
			assertThat(compte.getUtilisateurs().get(0).getCanAppro()).isFalse();
			assertThat(compte.getUtilisateurs().get(0).getCanSettle()).isTrue();
		}

		@Test
		@DisplayName("canAppro utilisateur est conservé si le compte l'autorise")
		void canApproConserve() {
			CompteEntity compte = compte(0L, true, false);
			CompteRequest request = CompteRequest.builder()
					.intitule("Caisse").numero("C-001").canAppro(true)
					.utilisateurs(List.of(CompteUtilisateurRequest.builder()
							.utilisateurId(7L).canAppro(true).build()))
					.build();
			when(compteMapper.toEntity(request)).thenReturn(compte);
			when(userRepository.findById(7L)).thenReturn(Optional.of(utilisateur));
			when(compteRepository.save(compte)).thenReturn(compte);
			when(compteMapper.toDto(compte)).thenReturn(Compte.builder().build());

			service.createCompte(request, null);

			assertThat(compte.getUtilisateurs().get(0).getCanAppro()).isTrue();
		}

		@Test
		@DisplayName("la mise à jour remplace intégralement la liste des utilisateurs habilités")
		void miseAJourRemplace() {
			CompteEntity compte = compte(0L, true, false);
			compte.getUtilisateurs().add(CompteUtilisateurEntity.builder().id(99L).build());
			CompteRequest request = CompteRequest.builder()
					.intitule("Caisse").numero("C-001").canAppro(true)
					.utilisateurs(List.of(CompteUtilisateurRequest.builder().utilisateurId(7L).build()))
					.build();
			when(compteRepository.findById(1L)).thenReturn(Optional.of(compte));
			when(userRepository.findById(7L)).thenReturn(Optional.of(utilisateur));
			when(compteRepository.save(compte)).thenReturn(compte);
			when(compteMapper.toDto(compte)).thenReturn(Compte.builder().build());

			service.updateCompte(1L, request, null);

			assertThat(compte.getUtilisateurs()).hasSize(1);
			assertThat(compte.getUtilisateurs().get(0).getUtilisateur()).isSameAs(utilisateur);
		}

		@Test
		@DisplayName("un utilisateur habilité inconnu lève 404")
		void utilisateurInconnu() {
			CompteEntity compte = compte(0L, true, false);
			CompteRequest request = CompteRequest.builder()
					.intitule("Caisse").numero("C-001").canAppro(true)
					.utilisateurs(List.of(CompteUtilisateurRequest.builder().utilisateurId(99L).build()))
					.build();
			when(compteMapper.toEntity(request)).thenReturn(compte);
			when(compteRepository.save(compte)).thenReturn(compte);
			when(userRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createCompte(request, null))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("Utilisateur avec l'id 99");
		}

		@Test
		@DisplayName("un managerId inconnu lève 404")
		void managerInconnu() {
			CompteEntity compte = compte(0L, false, false);
			CompteRequest request = CompteRequest.builder()
					.intitule("Caisse").numero("C-001").canAppro(false).managerId(99L).build();
			when(compteMapper.toEntity(request)).thenReturn(compte);
			when(userRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.createCompte(request, null))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("la mise à jour sans managerId détache le manager")
		void managerDetache() {
			CompteEntity compte = compte(0L, false, false);
			compte.setManager(utilisateur);
			CompteRequest request = CompteRequest.builder()
					.intitule("Caisse").numero("C-001").canAppro(false).build();
			when(compteRepository.findById(1L)).thenReturn(Optional.of(compte));
			when(compteRepository.save(compte)).thenReturn(compte);
			when(compteMapper.toDto(compte)).thenReturn(Compte.builder().build());

			service.updateCompte(1L, request, null);

			assertThat(compte.getManager()).isNull();
		}
	}

	@Nested
	@DisplayName("Mes comptes")
	class MesComptes {

		@Test
		@DisplayName("un encaissement ne propose que les comptes ouverts à l'approvisionnement")
		void encaissement() {
			CompteEntity compte = compte(0L, true, false);
			when(securityService.getUtilisateurConnecte()).thenReturn(utilisateur);
			when(compteUtilisateurRepository.findByUtilisateurIdAndCanApproTrueAndCompteCanApproTrue(7L))
					.thenReturn(List.of(habilitation(compte, true, false)));
			when(compteMapper.toDto(compte)).thenReturn(Compte.builder().id(1L).build());

			assertThat(service.getMesComptes(true)).hasSize(1);
			verify(compteUtilisateurRepository, never()).findByUtilisateurId(any());
		}

		@Test
		@DisplayName("un décaissement propose tous les comptes attribués")
		void decaissement() {
			CompteEntity compte = compte(0L, false, false);
			when(securityService.getUtilisateurConnecte()).thenReturn(utilisateur);
			when(compteUtilisateurRepository.findByUtilisateurId(7L))
					.thenReturn(List.of(habilitation(compte, false, false)));
			when(compteMapper.toDto(compte)).thenReturn(Compte.builder().id(1L).build());

			assertThat(service.getMesComptes(false)).hasSize(1);
		}
	}

	@Nested
	@DisplayName("Consultation")
	class Consultation {

		@Test
		@DisplayName("getCompteById lève 404 sur un id inconnu")
		void parId() {
			when(compteRepository.findById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getCompteById(99L))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("getCompteByNumero lève 404 sur un numéro inconnu")
		void parNumero() {
			when(compteRepository.findByNumero("C-404")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getCompteByNumero("C-404"))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("C-404");
		}

		@Test
		@DisplayName("une ligne appartenant à un autre compte est traitée comme introuvable")
		void ligneDUnAutreCompte() {
			LigneCompteEntity ligne = LigneCompteEntity.builder()
					.id(5L).compte(CompteEntity.builder().id(2L).build()).build();
			when(ligneCompteRepository.findById(5L)).thenReturn(Optional.of(ligne));

			assertThatThrownBy(() -> service.getLigneById(1L, 5L))
					.isInstanceOf(ResourceNotFoundException.class)
					.hasMessageContaining("non trouvée pour le compte 1");
		}

		@Test
		@DisplayName("lister les lignes d'un compte inconnu lève 404")
		void lignesCompteInconnu() {
			var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
			when(compteRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.getLignesByCompte(99L, pageable))
					.isInstanceOf(ResourceNotFoundException.class);
		}

		@Test
		@DisplayName("supprimer un compte inconnu lève 404")
		void delete_absent() {
			when(compteRepository.existsById(99L)).thenReturn(false);

			assertThatThrownBy(() -> service.deleteCompte(99L))
					.isInstanceOf(ResourceNotFoundException.class);
			verify(compteRepository, never()).deleteById(any());
		}
	}

	@Nested
	@DisplayName("Création d'un compte : montant initial")
	class CreationCompte {

		private CompteRequest requete(Long balance) {
			return CompteRequest.builder()
					.intitule("Caisse principale").numero("C-001")
					.balance(balance).canAppro(true).canBeNegative(false)
					.build();
		}

		private void stubCreation() {
			when(compteMapper.toEntity(any(CompteRequest.class)))
					.thenAnswer(i -> CompteEntity.builder().id(1L).build());
			when(compteRepository.save(any(CompteEntity.class))).thenAnswer(i -> i.getArgument(0));
		}

		@Test
		@DisplayName("sans montant, le compte est créé à 0 sans opération")
		void sansMontant() {
			stubCreation();

			service.createCompte(requete(null), null);

			ArgumentCaptor<CompteEntity> captor = ArgumentCaptor.forClass(CompteEntity.class);
			verify(compteRepository, atLeastOnce()).save(captor.capture());
			assertThat(captor.getValue().getBalance()).isZero();
			verify(ligneCompteRepository, never()).save(any(LigneCompteEntity.class));
		}

		@Test
		@DisplayName("un montant à 0 est traité comme une absence de montant")
		void montantZero() {
			stubCreation();

			service.createCompte(requete(0L), null);

			verify(ligneCompteRepository, never()).save(any(LigneCompteEntity.class));
		}

		@Test
		@DisplayName("un montant renseigné crée une ligne d'approvisionnement initial")
		void avecMontant() {
			stubCreation();
			when(securityService.getUtilisateurConnecte()).thenReturn(utilisateur);
			when(ligneCompteRepository.save(any(LigneCompteEntity.class))).thenAnswer(i -> i.getArgument(0));

			service.createCompte(requete(500_000L), null);

			ArgumentCaptor<LigneCompteEntity> ligneCaptor = ArgumentCaptor.forClass(LigneCompteEntity.class);
			verify(ligneCompteRepository).save(ligneCaptor.capture());
			LigneCompteEntity ligne = ligneCaptor.getValue();
			assertThat(ligne.getType()).isEqualTo(CompteLigneType.APPROVISIONNEMENT);
			assertThat(ligne.getObjet()).isEqualTo("APPROVISIONNEMENT INITIAL");
			assertThat(ligne.getMontant()).isEqualTo(500_000L);
			assertThat(ligne.getBalanceAvant()).isZero();
			assertThat(ligne.getUtilisateur()).isSameAs(utilisateur);
			assertThat(ligne.getCompte().getBalance()).isEqualTo(500_000L);
		}

		@Test
		@DisplayName("un montant négatif est refusé si le compte n'autorise pas le découvert")
		void montantNegatifInterdit() {
			stubCreation();

			assertThatThrownBy(() -> service.createCompte(requete(-1_000L), null))
					.isInstanceOf(BadRequestException.class);
			verify(ligneCompteRepository, never()).save(any(LigneCompteEntity.class));
		}
	}
}
