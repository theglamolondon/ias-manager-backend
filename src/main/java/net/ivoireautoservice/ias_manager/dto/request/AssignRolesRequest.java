package net.ivoireautoservice.ias_manager.dto.request;

import lombok.*;

import java.util.HashSet;
import java.util.Set;

/** Remplace l'ensemble des rôles directs d'un utilisateur. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssignRolesRequest {

    @Builder.Default
    private Set<Long> roleIds = new HashSet<>();
}
