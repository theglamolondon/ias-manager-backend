package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class LigneFacture {
	private Long id;
	private String reference;
	private Long qte;
	private Long prixUnitaire;
	private Float remise;
	private String designation;
	private Long montantHt;
	private TypeTarificationEnum typeTarification;
	private Long factureId;
	private Long produitId;
	private String produitDesignation;
}