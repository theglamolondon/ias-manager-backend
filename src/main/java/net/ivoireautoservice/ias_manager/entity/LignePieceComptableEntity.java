package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LIGNES_PIECE_COMPTABLE")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"pieceComptable", "produit"})
@EqualsAndHashCode(exclude = {"pieceComptable", "produit"})
public class LignePieceComptableEntity {

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
	@JoinColumn(name = "piece_comptable_id", referencedColumnName = "id", nullable = false)
	private PieceComptableEntity pieceComptable;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produit_id", referencedColumnName = "id")
	private ProduitEntity produit;
}
