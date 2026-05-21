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
@ToString(exclude = {"produit", "livraisonFournisseur", "ligneBonCommande"})
@EqualsAndHashCode(callSuper = true, exclude = {"produit", "livraisonFournisseur", "ligneBonCommande"})
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ligne_bon_commande_id", referencedColumnName = "id")
	private LigneBonCommandeEntity ligneBonCommande;
}
