package net.ivoireautoservice.ias_manager.config;

import com.ibm.icu.text.RuleBasedNumberFormat;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MoneyUtils {

	private final RuleBasedNumberFormat formatter =
			new RuleBasedNumberFormat(Locale.FRENCH, RuleBasedNumberFormat.SPELLOUT);

	public String montantEnLettre(double montant) {
		String lettres = formatter.format(montant);
		lettres = lettres.substring(0, 1).toUpperCase() + lettres.substring(1).toLowerCase();
		return lettres + " Francs CFA";
	}
}