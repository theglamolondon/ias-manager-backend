package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MissionRequest {
	private Long reference;
	private LocalDateTime dhmsDebutPrevi;
	private LocalDateTime dhmsFinPrevi;
	private LocalDateTime dhmsDebutReel;
	private LocalDateTime dhmsFinReel;
	private String itineraire;
	private Boolean isInterieur;
	private Boolean withChauffeur;
	private Boolean isConfirmer;
	private Long perdiem;
	private Long kilometrageDepart;
	private Long kilometrageArrive;

	@NotNull(message = "Le véhicule est obligatoire")
	private Long vehiculeId;

	private Long chauffeurId;
}
