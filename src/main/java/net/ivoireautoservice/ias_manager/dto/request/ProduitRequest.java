package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProduitRequest {

	@NotBlank(message = "La référence est obligatoire")
	private String reference;

	@NotBlank(message = "La désignation est obligatoire")
	private String designation;

	private Long prixUnitaire;

	private Long stock;

	@NotNull(message = "La famille est obligatoire")
	private Long familleId;
}
