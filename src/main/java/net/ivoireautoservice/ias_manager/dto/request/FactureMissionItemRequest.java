package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Item d'une facture mission groupée : une mission (à tarification INDEFINIE,
 * en cours) à inclure dans la facture, avec ses coûts de location ajustables
 * au moment de la facturation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FactureMissionItemRequest {

	@NotNull(message = "L'identifiant de la mission est requis")
	private Long missionId;

	/** Tarif mensuel de la mission, modifiable à la facturation. */
	@NotNull(message = "Le tarif est requis")
	private BigDecimal tarif;

	/** Nombre de mois facturés sur cette mission. */
	@NotNull(message = "La durée de location est requise")
	private Long dureeLocation;

	/** Perdiem journalier (optionnel, ignoré si la mission n'a pas de chauffeur). */
	private BigDecimal perdiem;

	/** Nombre de jours de perdiem (optionnel). */
	private Long dureePerdiem;
}
