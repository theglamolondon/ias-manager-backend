package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeVehicule {
	private Long id;
	private String libelle;
	private Long categorieId;
	private String categorieLibelle;
}
