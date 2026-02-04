package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDateTime;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class LivraisonFournisseur {
	private Long id;
	private String numero;
	private LocalDateTime dhmsLivraison;
}
