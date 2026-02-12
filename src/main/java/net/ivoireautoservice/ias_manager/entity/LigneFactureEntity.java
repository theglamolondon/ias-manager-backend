package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LIGNES_FACTURE")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"facture", "produit"})
@EqualsAndHashCode(exclude = {"facture", "produit"})
public class LigneFactureEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String reference;

	private Long qte;

	private Long prixUnitaire;

	private Float remise;

	private String designation;

	private Long montantHt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "facture_id", referencedColumnName = "id", nullable = false)
	private FactureEntity facture;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produit_id", referencedColumnName = "id")
	private ProduitEntity produit;
}