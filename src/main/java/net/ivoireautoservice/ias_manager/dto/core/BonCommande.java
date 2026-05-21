package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class BonCommande {
	private Long id;
	private LocalDateTime createdAt;
	private String numero;
	private LocalDate dateCommande;
	private LocalDate validite;
	private String objet;
	private Long montantHt;
	private Float tva;
	private Long montantTtc;
	private BonCommandeStatusEnum statut;
	private Long partenaireId;
	private String partenaireRaisonSociale;
	private List<LigneBonCommande> items;
}
