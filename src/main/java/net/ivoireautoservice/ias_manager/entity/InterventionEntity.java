package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.InterventionStatut;

import java.time.LocalDate;

@Entity
@Table(name = "INTERVENTIONS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"typeIntervention", "vehicule", "garage", "comptePaiement"})
@EqualsAndHashCode(callSuper = true, exclude = {"typeIntervention", "vehicule", "garage", "comptePaiement"})
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private InterventionStatut statut = InterventionStatut.CREEE;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_intervention_id", nullable = false)
	private TypeInterventionEntity typeIntervention;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicule_id", nullable = false)
	private VehiculeEntity vehicule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fournisseur_id")
	private PartenaireEntity garage;

	/**
	 * Date d'enregistrement de la dépense en trésorerie. Non nulle = intervention payée.
	 * Le paiement est <b>dissocié de la clôture</b> : une intervention peut être clôturée
	 * sans être payée, et payée sans être clôturée.
	 */
	@Column(name = "dhms_paiement")
	private LocalDate dhmsPaiement;

	/** Compte débité lors du paiement (trace comptable, renseigné avec {@link #dhmsPaiement}). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "paiement_compte_id")
	private CompteEntity comptePaiement;
}
