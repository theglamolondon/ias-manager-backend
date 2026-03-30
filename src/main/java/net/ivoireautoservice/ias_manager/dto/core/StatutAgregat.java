package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatutAgregat {
	private FactureStatusEnum statut;
	private long nombre;
	private long montant;
}
