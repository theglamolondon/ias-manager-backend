package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "LIVRAISONS_FOURNISSEUR")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LivraisonFournisseurEntity extends BaseLivraisonEntity {

	@Column(unique = true)
	private String numero;
}
