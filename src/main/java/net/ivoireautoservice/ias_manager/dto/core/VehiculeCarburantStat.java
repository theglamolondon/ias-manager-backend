package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VehiculeCarburantStat {
	private String typeCarburant;
	private Long nombre;
}