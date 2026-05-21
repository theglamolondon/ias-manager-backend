package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LivraisonFournisseurItemRequest {

	@NotNull(message = "La ligne du bon de commande est obligatoire")
	private Long ligneBonCommandeId;

	@NotNull(message = "La quantité livrée est obligatoire")
	@Positive(message = "La quantité livrée doit être strictement positive")
	private Long quantite;
}
