package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepenseMission {
	private Long id;
	private String libelle;
	private Long montant;
	private Long typeDepenseId;
	private String typeDepenseLibelle;
}
