package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Role {
    private Long id;
    private String nom;
    private String libelle;
    private String description;
    private Boolean systemRole;
    private Set<PermissionEnum> permissions;
}
