package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AffecterChauffeurRequest {

    /** ID du chauffeur à affecter. Null pour retirer le chauffeur de la mission. */
    private Long chauffeurId;

    /** Per diem journalier du chauffeur en FCFA. Null si pas de chauffeur. */
    private BigDecimal perdiem;
}
