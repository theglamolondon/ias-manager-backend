package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Site {
	private Long id;
	private String raisonSociale;
	private String logo;
	private String devise;
	private BigDecimal supIsInterieur;
	private BigDecimal supIsExterieur;
}
