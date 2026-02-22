package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InterventionRequest {

	private LocalDate dhmsDebut;
	private LocalDate dhmsFin;
	private String objet;
	private String details;
	private Long cout;

	@NotNull(message = "Le type d'intervention est obligatoire")
	private Long typeInterventionId;

	@NotNull(message = "Le véhicule est obligatoire")
	private Long vehiculeId;

	private Long fournisseurId;
}
