package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeVehiculeRequest {

	private Long id;

	@NotBlank(message = "Le libellé est obligatoire")
	private String libelle;

	private BigDecimal prixJournalier;

	private BigDecimal prixMensuel;

	@NotNull(message = "La catégorie est obligatoire")
	private Long categorieId;
}
