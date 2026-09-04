package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.LocalisationMissionEnum;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;

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
	private TypeTarificationEnum typeTarification;
	private String codeMission;
	private LocalDateTime dhmsDebutPrevi;
	private LocalDateTime dhmsFinPrevi;
	private LocalDateTime dhmsDebutReel;
	private LocalDateTime dhmsFinReel;
	private LocalDateTime dhmsAnnulation;
	private String motifAnnulation;
	private String destination;
	private LocalisationMissionEnum localisation;
	private Boolean withChauffeur;
	private Boolean isSousTraitee;
	private String detailsVehiculeSousTraitance;
	private BigDecimal tarif;
	private BigDecimal montantTotalHT;
	private Long dureeLocation;
	private Long kilometrageDepart;
	private Long kilometrageArrive;
	private String observations;
	private Vehicule vehicule;
	private Chauffeur chauffeur;
	private Partenaire client;
	private List<PhotoMission> photos;
	private Facture facture;
}
