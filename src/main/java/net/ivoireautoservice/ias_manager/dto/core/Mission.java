package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.TypeMissionEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Mission {
	private Long id;
	private Long reference;
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
	private Vehicule vehicule;
	private Chauffeur chauffeur;
	private Partenaire client;
	private List<DepenseMission> depenses;
	private List<Media> medias;
}
