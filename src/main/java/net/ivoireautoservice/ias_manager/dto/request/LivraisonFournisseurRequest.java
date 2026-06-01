package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LivraisonFournisseurRequest {

	private String numero;

	private String objet;

	private LocalDateTime dhmsLivraison;

	@NotNull(message = "Le bon de commande est obligatoire")
	private Long bonCommandeId;

	@NotEmpty(message = "Au moins une ligne livrée est requise")
	@Valid
	private List<LivraisonFournisseurItemRequest> items;
}
