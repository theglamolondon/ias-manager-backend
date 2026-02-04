package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeVehiculeRequest {

	@NotBlank(message = "Le libellé est obligatoire")
	private String libelle;

	@NotNull(message = "La catégorie est obligatoire")
	private Long categorieId;
}
