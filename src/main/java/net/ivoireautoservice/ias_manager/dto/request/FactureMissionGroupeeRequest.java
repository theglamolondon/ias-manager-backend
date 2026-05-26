package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Requête de génération d'une facture client regroupant plusieurs missions
 * non encore facturées pour un même client (tous types de tarification
 * confondus). Les coûts de location sont passés explicitement par mission
 * pour permettre leur ajustement à la facturation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FactureMissionGroupeeRequest {

	@NotNull(message = "Le client est requis")
	private Long partenaireId;

	private String objet;

	private Float tva;

	private LocalDate delaiLivraison;

	private LocalDate validite;

	@Valid
	@NotEmpty(message = "Au moins une mission est requise")
	private List<FactureMissionItemRequest> missions;
}
