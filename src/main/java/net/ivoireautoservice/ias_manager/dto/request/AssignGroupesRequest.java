package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

import java.util.HashSet;
import java.util.Set;

/** Remplace l'ensemble des groupes d'un utilisateur. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssignGroupesRequest {

    @Builder.Default
    private Set<Long> groupeIds = new HashSet<>();
}
