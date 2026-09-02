package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "lignes_bon_commande")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"bonCommande", "produit"})
@EqualsAndHashCode(callSuper = true, exclude = {"bonCommande", "produit"})
public class LigneBonCommandeEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String reference;

	private Long qte;

	@Column(name = "qte_livree")
	private Long qteLivree;

	private Long prixUnitaire;

	private Float remise;

	private String designation;

	private Long montantHt;

	@Column(name = "extra_ref")
	private String extraRef;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bon_commande_id", referencedColumnName = "id", nullable = false)
	private BonCommandeEntity bonCommande;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produit_id", referencedColumnName = "id")
	private ProduitEntity produit;
}
