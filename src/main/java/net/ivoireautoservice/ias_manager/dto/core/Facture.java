package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class Facture {
	private Long id;
	private String numProforma;
	private LocalDateTime dhmsCreationPiece;
	private String numFacture;
	private LocalDateTime dhmsFacture;
	private Long montantHt;
	private Float tva;
	private Long montantTtc;
	private LocalDate delaiLivraison;
	private LocalDate validite;
	private String objet;
	private Boolean factureClient;
	private FactureStatusEnum statut;
	private Long partenaireId;
	private String partenaireRaisonSociale;
	private List<LigneFacture> items;
	private Livraison livraison;
}