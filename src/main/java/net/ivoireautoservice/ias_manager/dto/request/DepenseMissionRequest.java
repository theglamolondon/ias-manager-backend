package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepenseMissionRequest {
	private String libelle;

	@NotNull(message = "Le montant est obligatoire")
	private Long montant;

	@NotNull(message = "Le type de dépense est obligatoire")
	private Long typeDepenseId;

	@NotNull(message = "Le compte à débiter est obligatoire")
	private Long compteId;
}
