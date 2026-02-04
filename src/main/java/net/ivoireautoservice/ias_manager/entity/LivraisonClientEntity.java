package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "LIVRAISONS_CLIENT")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LivraisonClientEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDateTime dhmsLivraison;
}
