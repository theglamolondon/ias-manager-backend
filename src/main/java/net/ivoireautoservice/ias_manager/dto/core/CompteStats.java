package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompteStats {
	private long total;
	private long balanceTotale;
	private long totalSoldesPositifs;
	private long totalSoldesNegatifs;
	private long facturesImpayeesNombre;
	private long facturesImpayeesMontant;

	/**
	 * Dépenses saisies sans imputation depuis 30 jours. Non nul, il mesure l'écart
	 * entre ce qui sort de caisse et ce qui est rattaché à un véhicule — donc ce qui
	 * manque aux coûts de la flotte.
	 */
	private long depensesNonImputees;
}
