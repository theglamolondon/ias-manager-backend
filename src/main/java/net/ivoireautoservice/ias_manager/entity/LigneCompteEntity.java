package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;
import net.ivoireautoservice.ias_manager.enums.LigneCompteOrigine;

import java.time.LocalDateTime;

@Entity
@Table(name = "lignes_compte")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"utilisateur", "compte", "facture", "typeDepense", "vehicule", "mission"})
@EqualsAndHashCode(callSuper = true, exclude = {"utilisateur", "compte", "facture", "typeDepense", "vehicule", "mission"})
public class LigneCompteEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "utilisateur_id", nullable = false)
	private Utilisateur utilisateur;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "compte_id", nullable = false)
	private CompteEntity compte;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "facture_id")
	private FactureEntity facture;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CompteLigneType type;

	/**
	 * Nature de la dépense (carburant, péage, assurance...). Renseigné sur les seules
	 * dépenses saisies en trésorerie ; nul sur les mouvements générés et sur tout ce
	 * qui n'est pas une dépense.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_depense_id")
	private TypeDepenseEntity typeDepense;

	/**
	 * Véhicule supportant la dépense. <b>Toujours renseigné dès qu'une imputation
	 * existe</b>, y compris lorsque la saisie s'est faite par la mission : le véhicule
	 * d'une mission est mutable ({@code changerVehicule}), une résolution à la lecture
	 * déplacerait donc rétroactivement des frais déjà engagés vers un autre véhicule.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicule_id")
	private VehiculeEntity vehicule;

	/** Mission à laquelle la dépense se rattache, quand elle a été saisie par cet axe. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id")
	private MissionEntity mission;

	/** Provenance du mouvement — détermine s'il porte ou non la valeur analytique. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LigneCompteOrigine origine;

	@Column(name = "dhms_operation", nullable = false)
	private LocalDateTime dhmsOperation;

	private String objet;

	@Column(nullable = false)
	private Long montant;

	@Column(name = "balance_avant", nullable = false)
	private Long balanceAvant;

	private String observation;
}
