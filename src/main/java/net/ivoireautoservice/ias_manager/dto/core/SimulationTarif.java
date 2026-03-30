package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.TypeTarificationEnum;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SimulationTarif {
	private Long duree;
	private BigDecimal tarifMinimum;
	private BigDecimal tarifUnitaire;
	private TypeTarificationEnum typeTarification;
}
