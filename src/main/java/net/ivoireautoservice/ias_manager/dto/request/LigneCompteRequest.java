package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LigneCompteRequest {

	@NotNull(message = "L'utilisateur est obligatoire")
	private Long utilisateurId;

	private String objet;

	@NotNull(message = "Le montant est obligatoire")
	private Long montant;

	private String observation;
}
