package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FamilleProduitRequest {

	private Long id;

	@NotBlank(message = "Le libellé est obligatoire")
	private String libelle;
}
