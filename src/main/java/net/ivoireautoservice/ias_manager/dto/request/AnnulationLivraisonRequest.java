package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnnulationLivraisonRequest {

	@NotNull(message = "Le statut cible du bon de commande est obligatoire")
	private BonCommandeStatusEnum statutBcCible;
}
