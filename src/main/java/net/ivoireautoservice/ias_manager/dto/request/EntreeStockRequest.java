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
public class EntreeStockRequest {

	private String numeroLivraison;

	private LocalDateTime dhmsLivraison;

	@NotEmpty(message = "La liste des entrées est obligatoire")
	@Valid
	private List<LigneEntree> lignes;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class LigneEntree {

		@NotNull(message = "Le produit est obligatoire")
		private Long produitId;

		@NotNull(message = "La quantité est obligatoire")
		private Long quantite;
	}
}