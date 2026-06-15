package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

/**
 * Représentation d'une permission du catalogue, exposée au frontend pour
 * construire l'UI d'attribution (cases à cocher).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionInfo {
    private String name;
    private String module;
    private String ressource;
    private String libelle;
}
