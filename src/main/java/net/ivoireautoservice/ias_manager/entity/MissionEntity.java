package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MISSIONS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"vehicule", "chauffeur", "client"})
@EqualsAndHashCode(callSuper = true, exclude = {"vehicule", "chauffeur", "client"})
public class MissionEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long reference;

	private String codeMission;

	@Column(name = "dhms_debut_previ")
	private LocalDateTime dhmsDebutPrevi;

	@Column(name = "dhms_fin_previ")
	private LocalDateTime dhmsFinPrevi;

	@Column(name = "dhms_debut_reel")
	private LocalDateTime dhmsDebutReel;

	@Column(name = "dhms_fin_reel")
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

	@Column(columnDefinition = "TEXT")
	private String observations;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicule_id", nullable = false)
	private VehiculeEntity vehicule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chauffeur_id")
	private ChauffeurEntity chauffeur;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_id")
	private PartenaireEntity client;
}
