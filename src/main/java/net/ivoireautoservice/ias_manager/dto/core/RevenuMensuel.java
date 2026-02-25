package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevenuMensuel {
	private Integer mois;
	private Long montantFactures;
	private BigDecimal montantMissions;
	private BigDecimal total;
}