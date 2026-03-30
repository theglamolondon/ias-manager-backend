package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.TypePartenaireEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PartenaireRequest {

	@NotNull(message = "Le type de partenaire est obligatoire")
	private TypePartenaireEnum type;

	@NotBlank(message = "La raison sociale est obligatoire")
	private String raisonSociale;

	private String numRc;
	private String numCc;
	private String telephone1;
	private String telephone2;
	private String email1;
	private String email2;
	private String adresse;

	@NotNull(message = "Le champ isClient est obligatoire")
	private Boolean isClient;

	@NotNull(message = "Le champ isFournisseur est obligatoire")
	private Boolean isFournisseur;
}
