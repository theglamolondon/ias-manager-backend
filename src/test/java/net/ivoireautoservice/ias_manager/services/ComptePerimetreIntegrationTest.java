package net.ivoireautoservice.ias_manager.services;

import jakarta.persistence.EntityManager;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.entity.CompteEntity;
import net.ivoireautoservice.ias_manager.entity.CompteUtilisateurEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.repository.CompteRepository;
import net.ivoireautoservice.ias_manager.repository.CompteUtilisateurRepository;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie contre la base réelle les deux mécanismes qui ne peuvent pas l'être
 * avec des mocks : la formule Hibernate qui dérive {@code TRESORERIE_READ} du
 * rattachement à un compte, et le filtrage SQL par périmètre
 * ({@code userId = null} ⇒ tous les comptes).
 *
 * <p>Transactionnel : tout est annulé en fin de test.</p>
 */
@SpringBootTest
@Transactional
@DisplayName("Périmètre de trésorerie — intégration base de données")
class ComptePerimetreIntegrationTest {

	private static final Pageable PAGE = PageRequest.of(0, 50);

	@Autowired private CompteRepository compteRepository;
	@Autowired private CompteUtilisateurRepository compteUtilisateurRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private EntityManager entityManager;

	private Utilisateur affecte;
	private Utilisateur managerDuCompte;
	private Utilisateur etranger;
	private CompteEntity compteAffecte;
	private CompteEntity compteManage;

	private Utilisateur utilisateur(String prefixe) {
		return userRepository.save(Utilisateur.builder()
				.nom(prefixe).prenom("Test")
				.email(prefixe + "-" + UUID.randomUUID() + "@test.ci")
				.password("x")
				.build());
	}

	private CompteEntity compte(String intitule, Utilisateur manager, long balance) {
		return compteRepository.save(CompteEntity.builder()
				.intitule(intitule)
				.numero("T-" + UUID.randomUUID())
				.balance(balance)
				.canAppro(true)
				.canBeNegative(true)
				.manager(manager)
				.build());
	}

	@BeforeEach
	void setUp() {
		affecte = utilisateur("affecte");
		managerDuCompte = utilisateur("manager");
		etranger = utilisateur("etranger");

		compteAffecte = compte("Caisse affectée", null, 300_000L);
		compteManage = compte("Caisse managée", managerDuCompte, -50_000L);

		compteUtilisateurRepository.save(CompteUtilisateurEntity.builder()
				.compte(compteAffecte).utilisateur(affecte)
				.canAppro(true).canSettle(false)
				.build());

		entityManager.flush();
		entityManager.clear();
	}

	@Test
	@DisplayName("l'affectation à un compte dérive TRESORERIE_READ, son absence non")
	void permissionDeriveeDuRattachement() {
		assertThat(userRepository.findById(affecte.getId()).orElseThrow().getPermissionNames())
				.contains(PermissionEnum.TRESORERIE_READ.name());
		assertThat(userRepository.findById(managerDuCompte.getId()).orElseThrow().getPermissionNames())
				.contains(PermissionEnum.TRESORERIE_READ.name());
		assertThat(userRepository.findById(etranger.getId()).orElseThrow().getPermissionNames())
				.doesNotContain(PermissionEnum.TRESORERIE_READ.name());
	}

	@Test
	@DisplayName("la fin du rattachement retire la permission sans autre intervention")
	void permissionRetireeAvecLeRattachement() {
		compteUtilisateurRepository.deleteAll(
				compteUtilisateurRepository.findByCompteId(compteAffecte.getId()));
		entityManager.flush();
		entityManager.clear();

		assertThat(userRepository.findById(affecte.getId()).orElseThrow().getPermissionNames())
				.doesNotContain(PermissionEnum.TRESORERIE_READ.name());
	}

	@Test
	@DisplayName("la liste ne renvoie que les comptes rattachés à l'utilisateur")
	void listeRestreinteAuPerimetre() {
		assertThat(compteRepository.search(null, affecte.getId(), PAGE).getContent())
				.extracting(CompteEntity::getId)
				.containsExactly(compteAffecte.getId());

		assertThat(compteRepository.search(null, managerDuCompte.getId(), PAGE).getContent())
				.extracting(CompteEntity::getId)
				.containsExactly(compteManage.getId());

		assertThat(compteRepository.search(null, etranger.getId(), PAGE).getContent()).isEmpty();
	}

	@Test
	@DisplayName("un périmètre nul (trésorier en chef) voit les deux comptes")
	void perimetreNulVoitTout() {
		assertThat(compteRepository.search(null, null, PAGE).getContent())
				.extracting(CompteEntity::getId)
				.contains(compteAffecte.getId(), compteManage.getId());
	}

	@Test
	@DisplayName("la recherche par mot-clé s'applique à l'intérieur du périmètre")
	void rechercheDansLePerimetre() {
		assertThat(compteRepository.search("affectée", affecte.getId(), PAGE).getContent())
				.extracting(CompteEntity::getId)
				.containsExactly(compteAffecte.getId());

		assertThat(compteRepository.search("managée", affecte.getId(), PAGE).getContent()).isEmpty();
	}

	@Test
	@DisplayName("un compte hors périmètre est invisible, même par son id ou son numéro")
	void accesDirectHorsPerimetre() {
		assertThat(compteRepository.findVisibleById(compteAffecte.getId(), affecte.getId())).isPresent();
		assertThat(compteRepository.findVisibleById(compteAffecte.getId(), etranger.getId())).isEmpty();
		assertThat(compteRepository.findVisibleById(compteAffecte.getId(), null)).isPresent();

		String numero = compteAffecte.getNumero();
		assertThat(compteRepository.findVisibleByNumero(numero, affecte.getId())).isPresent();
		assertThat(compteRepository.findVisibleByNumero(numero, etranger.getId())).isEmpty();
	}

	@Test
	@DisplayName("les agrégats des cards sont calculés sur le seul périmètre de l'utilisateur")
	void statistiquesRestreintes() {
		Object[] stats = compteRepository.statistiques(affecte.getId()).get(0);

		assertThat(((Number) stats[0]).longValue()).isEqualTo(1L);
		assertThat(((Number) stats[1]).longValue()).isEqualTo(300_000L);
		assertThat(((Number) stats[2]).longValue()).isEqualTo(300_000L);
		assertThat(((Number) stats[3]).longValue()).isZero();

		Object[] statsManager = compteRepository.statistiques(managerDuCompte.getId()).get(0);
		assertThat(((Number) statsManager[1]).longValue()).isEqualTo(-50_000L);
		assertThat(((Number) statsManager[3]).longValue()).isEqualTo(-50_000L);

		Object[] statsEtranger = compteRepository.statistiques(etranger.getId()).get(0);
		assertThat(((Number) statsEtranger[0]).longValue()).isZero();
		assertThat(((Number) statsEtranger[1]).longValue()).isZero();
	}
}
