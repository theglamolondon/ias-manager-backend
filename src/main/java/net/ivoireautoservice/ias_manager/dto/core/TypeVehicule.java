package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TypeVehicule {
	private Long id;
	private String libelle;
	private BigDecimal prixJournalier;
	private BigDecimal prixMensuel;
	private Categorie categorie;
}
