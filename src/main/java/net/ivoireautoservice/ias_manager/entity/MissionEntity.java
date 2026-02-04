package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "MISSIONS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"vehicule", "chauffeur"})
@EqualsAndHashCode(exclude = {"vehicule", "chauffeur"})
public class MissionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long reference;

	@Column(name = "dhms_debut_previ")
	private LocalDateTime dhmsDebutPrevi;

	@Column(name = "dhms_fin_previ")
	private LocalDateTime dhmsFinPrevi;

	@Column(name = "dhms_debut_reel")
	private LocalDateTime dhmsDebutReel;

	@Column(name = "dhms_fin_reel")
	private LocalDateTime dhmsFinReel;

	private String itineraire;

	private Boolean isInterieur;

	private Boolean withChauffeur;

	private Boolean isConfirmer;

	private Long perdiem;

	private Long kilometrageDepart;

	private Long kilometrageArrive;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicule_id", nullable = false)
	private VehiculeEntity vehicule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chauffeur_id")
	private ChauffeurEntity chauffeur;
}
