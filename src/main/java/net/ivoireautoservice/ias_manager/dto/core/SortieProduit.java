package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class SortieProduit {
	private Long id;
	private Long quantite;
	private Long livraisonClientId;
	private Long produitId;
	private String produitDesignation;
}
