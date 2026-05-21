package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;
import net.ivoireautoservice.ias_manager.enums.StatutBonLivraisonEnum;

@Data @AllArgsConstructor @NoArgsConstructor @SuperBuilder @ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LivraisonFournisseurSummary extends LivraisonSummary {
	private String numero;
	private StatutBonLivraisonEnum statut;
	private Long bonCommandeId;
	private String bonCommandeNumero;
	private BonCommandeStatusEnum bonCommandeStatut;
	private String partenaireRaisonSociale;
	private Long montantHtEstime;
}
