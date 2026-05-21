package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.BonCommandeStatusEnum;
import net.ivoireautoservice.ias_manager.enums.StatutBonLivraisonEnum;

import java.time.LocalDateTime;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @SuperBuilder @ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LivraisonFournisseur extends Livraison {
	private String numero;
	private StatutBonLivraisonEnum statut;
	private LocalDateTime dateValidation;
	private LocalDateTime dateAnnulation;
	private Long bonCommandeId;
	private String bonCommandeNumero;
	private BonCommandeStatusEnum bonCommandeStatut;
	private String partenaireRaisonSociale;
	private List<EntreeProduit> entrees;
}
