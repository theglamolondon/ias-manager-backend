package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "INTERVENTIONS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"typeIntervention", "vehicule"})
@EqualsAndHashCode(callSuper = true, exclude = {"typeIntervention", "vehicule"})
public class InterventionEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "dhms_debut")
	private LocalDate dhmsDebut;

	@Column(name = "dhms_fin")
	private LocalDate dhmsFin;

	private String objet;

	private String details;

	private Long cout;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_intervention_id", nullable = false)
	private TypeInterventionEntity typeIntervention;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicule_id", nullable = false)
	private VehiculeEntity vehicule;
}
