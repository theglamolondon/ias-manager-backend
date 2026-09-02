package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;

import java.time.LocalDate;

@Entity
@Table(name = "bons_commande")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"partenaire", "createdBy"})
@EqualsAndHashCode(callSuper = true, exclude = {"partenaire", "createdBy"})
public class BonCommandeEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String numero;

	private LocalDate dateCommande;

	private LocalDate validite;

	private String objet;

	private Long montantHt;

	private Float tva;

	private Long montantTtc;

	@Enumerated(EnumType.STRING)
	private BonCommandeStatusEnum statut;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partenaire_id", referencedColumnName = "id", nullable = false)
	private PartenaireEntity partenaire;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_id", referencedColumnName = "id")
	private Utilisateur createdBy;
}
