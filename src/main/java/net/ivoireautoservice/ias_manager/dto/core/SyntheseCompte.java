package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

/**
 * Totaux d'un compte de trésorerie, calculés sur l'<b>intégralité</b> de ses
 * opérations et non sur la page affichée.
 *
 * <p>{@code totalDepenses} regroupe les décaissements — dépenses et remboursements,
 * qui débitent tous deux le compte — comme le fait le flux du rapport financier.
 * Chaque type reste exposé séparément, pour que l'affichage puisse les dissocier
 * sans nouvelle requête.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyntheseCompte {
	private long totalDepenses;
	private long totalApprovisionnements;
	private long totalRemboursements;
	private long totalSoldes;
	private long nombreOperations;
}
