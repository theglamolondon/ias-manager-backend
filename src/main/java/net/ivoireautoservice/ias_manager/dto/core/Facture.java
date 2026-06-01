package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;
import net.ivoireautoservice.ias_manager.enums.FactureNatureEnum;
import net.ivoireautoservice.ias_manager.enums.FactureTypeEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class Facture {
	private Long id;
	private LocalDateTime createdAt;
	private String numProforma;
	private String numFacture;
	private Long montantHt;
	private Float tva;
	private Long montantTtc;
	private LocalDate delaiLivraison;
	private LocalDate validite;
	private String objet;
	private String conditionsPaiement;
	private String statutLivraison;
	private Boolean factureClient;
	private FactureStatusEnum statut;
	private FactureNatureEnum nature;
	private FactureTypeEnum type;
	private Partenaire partenaire;
	private Long factureOrigineId;
	private String factureOrigineNumero;
	private Long createdById;
	private String createdByNom;
	private List<LigneFacture> items;
	private Livraison livraison;
}