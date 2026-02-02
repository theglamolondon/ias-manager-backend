package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompteRequest {

	@NotBlank(message = "L'intitulé est obligatoire")
	private String intitule;

	@NotBlank(message = "Le numéro est obligatoire")
	private String numero;

	private String description;

	@NotNull(message = "La balance est obligatoire")
	private Long balance;

	@NotNull(message = "Le champ isAppro est obligatoire")
	private Boolean isAppro;
}
