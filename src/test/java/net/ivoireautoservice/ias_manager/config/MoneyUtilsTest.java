package net.ivoireautoservice.ias_manager.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoneyUtils — montant en lettres")
class MoneyUtilsTest {

	private final MoneyUtils moneyUtils = new MoneyUtils();

	@Test
	@DisplayName("met une majuscule initiale et suffixe la devise")
	void montantEnLettre_formatte() {
		String resultat = moneyUtils.montantEnLettre(21);

		assertThat(resultat).startsWith("V");
		assertThat(resultat).endsWith(" Francs CFA");
	}

	@Test
	@DisplayName("zéro est libellé")
	void montantEnLettre_zero() {
		assertThat(moneyUtils.montantEnLettre(0)).isEqualTo("Zéro Francs CFA");
	}

	@Test
	@DisplayName("un montant à sept chiffres est intégralement écrit en lettres")
	void montantEnLettre_grandMontant() {
		String resultat = moneyUtils.montantEnLettre(1_500_000);

		assertThat(resultat).contains("million");
		assertThat(resultat).doesNotContain("1");
	}

	@Test
	@DisplayName("le reste du libellé est en minuscules")
	void montantEnLettre_resteEnMinuscule() {
		String resultat = moneyUtils.montantEnLettre(100);
		String sansDevise = resultat.substring(0, resultat.length() - " Francs CFA".length());

		assertThat(sansDevise.substring(1)).isEqualTo(sansDevise.substring(1).toLowerCase());
	}
}
