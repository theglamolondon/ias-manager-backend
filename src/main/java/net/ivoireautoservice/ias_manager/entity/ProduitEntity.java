package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PRODUITS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "famille")
@EqualsAndHashCode(exclude = "famille")
public class ProduitEntity {

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
}
