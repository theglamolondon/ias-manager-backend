package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompteUtilisateurRequest {

	@NotNull(message = "L'utilisateur est obligatoire")
	private Long utilisateurId;

	private Boolean canAppro;

	private Boolean canSettle;
}
