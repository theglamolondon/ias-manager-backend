package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sorties_produit")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"livraisonClient", "produit"})
@EqualsAndHashCode(callSuper = true, exclude = {"livraisonClient", "produit"})
public class SortieProduitEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long quantite;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "livraison_client_id", referencedColumnName = "id", nullable = false)
	private LivraisonClientEntity livraisonClient;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produit_id", referencedColumnName = "id", nullable = false)
	private ProduitEntity produit;
}
