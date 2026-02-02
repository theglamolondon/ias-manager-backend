package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Compte {
	private Long id;
	private String intitule;
	private String numero;
	private String description;
	private Long balance;
	private Boolean isAppro;
}
