package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PartenaireRequest {

	@NotBlank(message = "La raison sociale est obligatoire")
	private String raisonSociale;

	private String numRc;
	private String numCc;
	private String telephone1;
	private String telephone2;
	private String email1;
	private String email2;

	@NotNull(message = "Le champ isClient est obligatoire")
	private Boolean isClient;

	@NotNull(message = "Le champ isFournisseur est obligatoire")
	private Boolean isFournisseur;
}
