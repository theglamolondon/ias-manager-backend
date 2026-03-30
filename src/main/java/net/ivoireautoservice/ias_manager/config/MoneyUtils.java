package net.ivoireautoservice.ias_manager.config;

import com.ibm.icu.text.RuleBasedNumberFormat;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MoneyUtils {

	private final RuleBasedNumberFormat formatter =
			new RuleBasedNumberFormat(Locale.FRENCH, RuleBasedNumberFormat.SPELLOUT);

	public String montantEnLettre(double montant) {
		return formatter.format(montant).toUpperCase() + " FRANCS CFA";
	}
}