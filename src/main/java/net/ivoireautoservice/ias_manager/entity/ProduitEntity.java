package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "produits")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"famille", "image"})
@EqualsAndHashCode(callSuper = true, exclude = {"famille", "image"})
public class ProduitEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String reference;

	@Column(nullable = false)
	private String designation;

	private Long prixUnitaire;

	private Long stock;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "famille_id", referencedColumnName = "id")
	private FamilleProduitEntity famille;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_id")
	private MediaEntity image;
}
