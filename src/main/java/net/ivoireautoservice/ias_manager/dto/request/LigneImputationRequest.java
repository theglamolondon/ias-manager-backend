package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Correction de l'imputation analytique d'un mouvement de trésorerie déjà enregistré.
 * Volontairement limité à la nature et à l'axe : montant, compte, type et date sont
 * hors d'atteinte, car les modifier fausserait la balance du compte et les
 * {@code balanceAvant} de toutes les lignes postérieures.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LigneImputationRequest {

	@NotNull(message = "La nature de la dépense est obligatoire")
	private Long typeDepenseId;

	private Long vehiculeId;

	private Long missionId;

	private Boolean nonImputable;
}
