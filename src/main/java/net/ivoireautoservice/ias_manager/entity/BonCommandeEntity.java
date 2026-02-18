package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "BONS_COMMANDE")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = "partenaire")
@EqualsAndHashCode(callSuper = true, exclude = "partenaire")
public class BonCommandeEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDate dateCommande;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partenaire_id", referencedColumnName = "id", nullable = false)
	private PartenaireEntity partenaire;
}
