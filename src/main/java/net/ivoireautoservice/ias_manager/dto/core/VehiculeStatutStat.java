package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VehiculeStatutStat {
	private String statut;
	private Long nombre;
	private Double pourcentage;
}