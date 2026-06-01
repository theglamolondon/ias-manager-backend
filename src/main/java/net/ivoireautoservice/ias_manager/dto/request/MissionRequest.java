package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.LocalisationMissionEnum;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MissionRequest {
	private Long reference;

	@NotNull(message = "Le type de tarification est obligatoire")
	private TypeTarificationEnum typeTarification;
	private LocalDateTime dhmsDebutPrevi;
	private LocalDateTime dhmsFinPrevi;
	private LocalDateTime dhmsDebutReel;
	private LocalDateTime dhmsFinReel;
	private String destination;
	private LocalisationMissionEnum localisation;
	private Boolean withChauffeur;
	private Boolean isSousTraitee;
	private String detailsVehiculeSousTraitance;
	private BigDecimal perdiem;
	private BigDecimal totalPerdiem;
	private BigDecimal tarif;
	private BigDecimal montantTotalHT;
	private Long dureeLocation;
	private Long kilometrageDepart;
	private Long kilometrageArrive;
	private String observations;

	@NotNull(message = "Le véhicule est obligatoire")
	private Long vehiculeId;

	private Long chauffeurId;

	private Long clientId;

	private Long compteId;

	/**
	 * Si vrai (par défaut), une facture client est générée automatiquement à la
	 * création de la mission (sauf tarification INDEFINIE). Si faux, aucune
	 * facture n'est créée — elle pourra l'être manuellement plus tard.
	 */
	private Boolean genererFacture;
}
