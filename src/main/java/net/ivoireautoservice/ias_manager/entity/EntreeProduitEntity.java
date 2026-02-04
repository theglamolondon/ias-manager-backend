package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ENTREES_PRODUIT")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"produit", "livraisonFournisseur"})
@EqualsAndHashCode(exclude = {"produit", "livraisonFournisseur"})
public class EntreeProduitEntity {

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
