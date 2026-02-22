package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LigneCompteRequest {

	@NotNull(message = "Le type de mouvement est obligatoire")
	private CompteLigneType type;

	private String objet;

	@NotNull(message = "Le montant est obligatoire")
	@Positive(message = "Le montant doit être positif")
	private Long montant;

	private String observation;
}
