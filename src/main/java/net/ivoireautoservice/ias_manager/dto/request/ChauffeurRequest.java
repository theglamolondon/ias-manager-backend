package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChauffeurRequest {

	private Long employeId;

	@NotBlank(message = "Le numéro de permis est obligatoire")
	private String numeroPermis;

	private LocalDate expDatePermis;
	private String typePermis;
}
