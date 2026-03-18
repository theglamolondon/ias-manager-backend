package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeAssuranceRequest {

	private Long id;

	@NotBlank(message = "Le libellé est obligatoire")
	private String libelle;

	@Size(max = 10, message = "Le contact1 ne doit pas dépasser 10 caractères")
	private String contact1;

	@Size(max = 10, message = "Le contact2 ne doit pas dépasser 10 caractères")
	private String contact2;
}
