package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "LIVRAISONS_FOURNISSEUR")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LivraisonFournisseurEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String numero;

	private LocalDateTime dhmsLivraison;
}
