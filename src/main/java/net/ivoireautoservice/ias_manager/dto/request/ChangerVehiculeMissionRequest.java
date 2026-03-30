package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangerVehiculeMissionRequest {

	@NotNull(message = "L'identifiant de la mission est obligatoire")
	private Long missionId;

	@NotNull(message = "L'identifiant du nouveau véhicule est obligatoire")
	private Long nouveauVehiculeId;
}
