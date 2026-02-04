package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.time.LocalDate;

@Data @AllArgsConstructor @NoArgsConstructor @Builder @ToString
public class BonCommande {
	private Long id;
	private LocalDate dateCommande;
	private Long partenaireId;
	private String partenaireRaisonSociale;
}
