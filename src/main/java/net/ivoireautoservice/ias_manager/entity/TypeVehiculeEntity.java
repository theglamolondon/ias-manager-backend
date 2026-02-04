package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TYPES_VEHICULE")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "categorie")
@EqualsAndHashCode(exclude = "categorie")
public class TypeVehiculeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String libelle;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "categorie_id", referencedColumnName = "id", nullable = false)
	private CategorieEntity categorie;
}
