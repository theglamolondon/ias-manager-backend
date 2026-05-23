package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SiteRequest {

	@NotBlank(message = "La raison sociale est obligatoire")
	private String raisonSociale;

	private String logo;

	private String devise;

	@PositiveOrZero(message = "Le supplément intérieur doit être positif ou nul")
	private BigDecimal supIsInterieur;

	@PositiveOrZero(message = "Le supplément extérieur doit être positif ou nul")
	private BigDecimal supIsExterieur;
}
