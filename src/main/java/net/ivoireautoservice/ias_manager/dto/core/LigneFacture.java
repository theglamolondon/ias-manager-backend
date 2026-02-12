package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class LigneFacture {
	private Long id;
	private String reference;
	private Long qte;
	private Long prixUnitaire;
	private Float remise;
	private String designation;
	private Long montantHt;
	private Long factureId;
	private Long produitId;
	private String produitDesignation;
}