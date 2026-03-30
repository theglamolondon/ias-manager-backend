package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class LivraisonSummary {
	private Long id;
	private LocalDateTime dhmsLivraison;
	private Long factureId;
	private String factureNumProforma;
}
