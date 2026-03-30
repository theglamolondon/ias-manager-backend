package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "TYPES_VEHICULE")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = "categorie")
@EqualsAndHashCode(callSuper = true, exclude = "categorie")
public class TypeVehiculeEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String libelle;

	private BigDecimal prixJournalier;

	private BigDecimal prixMensuel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "categorie_id", referencedColumnName = "id", nullable = false)
	private CategorieEntity categorie;
}
