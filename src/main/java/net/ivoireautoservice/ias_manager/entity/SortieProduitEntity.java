package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SORTIES_PRODUIT")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"livraisonClient", "produit"})
@EqualsAndHashCode(exclude = {"livraisonClient", "produit"})
public class SortieProduitEntity {

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
