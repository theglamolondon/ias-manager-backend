package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ENTREES_PRODUIT")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"produit", "livraisonFournisseur"})
@EqualsAndHashCode(callSuper = true, exclude = {"produit", "livraisonFournisseur"})
public class EntreeProduitEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long quantite;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produit_id", referencedColumnName = "id", nullable = false)
	private ProduitEntity produit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "livraison_fournisseur_id", referencedColumnName = "id", nullable = false)
	private LivraisonFournisseurEntity livraisonFournisseur;
}
