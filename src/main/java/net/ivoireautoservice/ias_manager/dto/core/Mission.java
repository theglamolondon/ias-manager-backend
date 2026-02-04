package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Mission {
	private Long id;
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
	private Long vehiculeId;
	private String vehiculeImmatriculation;
	private Long chauffeurId;
	private String chauffeurNumeroPermis;
	private List<DepenseMission> depenses;
	private List<Media> medias;
}
