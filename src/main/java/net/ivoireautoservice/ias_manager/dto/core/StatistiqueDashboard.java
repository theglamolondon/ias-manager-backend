package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StatistiqueDashboard {
	private List<VehiculeStatutStat> vehiculesParStatut;
	private List<VehiculeCarburantStat> vehiculesParCarburant;
	private List<RevenuMensuel> revenusMensuels;
}