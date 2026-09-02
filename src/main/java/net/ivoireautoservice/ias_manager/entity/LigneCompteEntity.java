package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.CompteLigneType;

import java.time.LocalDateTime;

@Entity
@Table(name = "lignes_compte")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"utilisateur", "compte", "facture"})
@EqualsAndHashCode(callSuper = true, exclude = {"utilisateur", "compte", "facture"})
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

	@Column(name = "dhms_operation", nullable = false)
	private LocalDateTime dhmsOperation;

	private String objet;

	@Column(nullable = false)
	private Long montant;

	@Column(name = "balance_avant", nullable = false)
	private Long balanceAvant;

	private String observation;
}
