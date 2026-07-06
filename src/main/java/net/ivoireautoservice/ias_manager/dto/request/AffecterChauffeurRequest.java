package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AffecterChauffeurRequest {

    /** ID du chauffeur à affecter. Null pour retirer le chauffeur de la mission. */
    private Long chauffeurId;
}
