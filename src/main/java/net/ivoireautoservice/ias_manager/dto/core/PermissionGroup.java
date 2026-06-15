package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.List;

/**
 * Catalogue des permissions groupées par module, pour l'arbre de cases à cocher
 * de l'UI d'administration des rôles.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionGroup {
    private String module;
    private List<PermissionInfo> permissions;
}
