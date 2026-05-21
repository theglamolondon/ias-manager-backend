package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FactureFournisseurGroupeeRequest {

	@NotEmpty(message = "Au moins un bon de livraison est requis")
	private List<Long> livraisonIds;

	private String objet;
}
