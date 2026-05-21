package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class EntreeProduit {
	private Long id;
	private Long quantite;
	private Long produitId;
	private String produitDesignation;
	private Long livraisonFournisseurId;
	private Long ligneBonCommandeId;
}
