package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class Produit {
	private Long id;
	private String reference;
	private String designation;
	private Long prixUnitaire;
	private Long stock;
	private Long familleId;
	private String familleLibelle;
	private Media image;
}
