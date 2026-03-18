package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.TypeMissionEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MissionRequest {
	private Long reference;

	@NotNull(message = "Le type de mission est obligatoire")
	private TypeMissionEnum typeMission;
	private String codeMission;
	private LocalDateTime dhmsDebutPrevi;
	private LocalDateTime dhmsFinPrevi;
	private LocalDateTime dhmsDebutReel;
	private LocalDateTime dhmsFinReel;
	private String itineraire;
	private String destination;
	private Boolean isInterieur;
	private Boolean withChauffeur;
	private Boolean isConfirmer;
	private Boolean isSousTraitee;
	private String detailsVehiculeSousTraitance;
	private BigDecimal perdiem;
	private BigDecimal totalPerdiem;
	private BigDecimal tarifJournalier;
	private BigDecimal montantTotalHT;
	private Long dureeLocation;
	private Long kilometrageDepart;
	private Long kilometrageArrive;
	private String observations;

	@NotNull(message = "Le véhicule est obligatoire")
	private Long vehiculeId;

	private Long chauffeurId;

	private Long clientId;
}
