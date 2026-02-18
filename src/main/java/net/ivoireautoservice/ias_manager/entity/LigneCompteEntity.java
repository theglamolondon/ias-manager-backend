package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "LIGNES_COMPTE")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"utilisateur", "compte"})
@EqualsAndHashCode(callSuper = true, exclude = {"utilisateur", "compte"})
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

	@Column(name = "dhms_operation", nullable = false)
	private LocalDateTime dhmsOperation;

	private String objet;

	@Column(nullable = false)
	private Long montant;

	@Column(name = "balance_avant", nullable = false)
	private Long balanceAvant;

	private String observation;
}
