package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDateTime;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class LivraisonClient {
	private Long id;
	private LocalDateTime dhmsLivraison;
}
