package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EntreeProduitRequest {

	@NotNull(message = "La quantité est obligatoire")
	private Long quantite;

	@NotNull(message = "Le produit est obligatoire")
	private Long produitId;
}
