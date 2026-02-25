package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VehiculeStats {
	private long total;
	private List<VehiculeStatutStat> parStatut;
}
