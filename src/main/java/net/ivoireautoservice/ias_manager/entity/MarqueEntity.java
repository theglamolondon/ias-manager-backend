package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MARQUES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MarqueEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String libelle;
}
