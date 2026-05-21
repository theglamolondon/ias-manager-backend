package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class LigneBonCommande {
	private Long id;
	private String reference;
	private Long qte;
	private Long qteLivree;
	private Long prixUnitaire;
	private Float remise;
	private String designation;
	private Long montantHt;
	private String extraRef;
	private Long bonCommandeId;
	private Long produitId;
	private String produitDesignation;
}
