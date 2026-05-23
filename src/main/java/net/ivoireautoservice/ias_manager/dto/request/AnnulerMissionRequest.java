package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnnulerMissionRequest {

	/**
	 * Compte à débiter pour le remboursement.
	 *
	 * Obligatoire si la facture associée est une proforma déjà PAYÉE.
	 * Pour un reçu, le compte ayant reçu le paiement est utilisé automatiquement.
	 * Pour une proforma non payée, aucun remboursement n'est nécessaire.
	 */
	private Long compteId;

	private String motif;
}
