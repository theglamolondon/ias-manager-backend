package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangerVehiculeMissionRequest {

	@NotNull(message = "L'identifiant de la mission est obligatoire")
	private Long missionId;

	@NotNull(message = "L'identifiant du nouveau véhicule est obligatoire")
	private Long nouveauVehiculeId;

	/**
	 * Statut à attribuer à l'ancien véhicule après le changement.
	 * Valeurs acceptées : GARAGE, SINISTRE.
	 * Par défaut : GARAGE.
	 */
	private VehiculeStatusEnum nouveauStatutAncienVehicule;
}
