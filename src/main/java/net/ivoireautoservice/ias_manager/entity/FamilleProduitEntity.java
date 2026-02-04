package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "FAMILLES_PRODUIT")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FamilleProduitEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String libelle;
}
