package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.StatutBonLivraisonEnum;

import java.time.LocalDateTime;

@Entity
@Table(name = "livraisons_fournisseur")
@AssociationOverride(
		name = "facture",
		joinColumns = @JoinColumn(name = "facture_id", referencedColumnName = "id")
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true, exclude = "bonCommande")
@EqualsAndHashCode(callSuper = true, exclude = "bonCommande")
public class LivraisonFournisseurEntity extends BaseLivraisonEntity {

	@Column(unique = true)
	private String numero;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatutBonLivraisonEnum statut;

	private LocalDateTime dateValidation;

	private LocalDateTime dateAnnulation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bon_commande_id", referencedColumnName = "id", nullable = true)
	private BonCommandeEntity bonCommande;
}
