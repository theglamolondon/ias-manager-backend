package net.ivoireautoservice.ias_manager.controller;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
import net.ivoireautoservice.ias_manager.dto.core.PermissionGroup;
import net.ivoireautoservice.ias_manager.dto.core.PermissionInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expose le catalogue des permissions (vocabulaire fixe défini en code),
 * groupé par module, pour que l'UI d'administration construise l'arbre de
 * cases à cocher lors de la création/édition d'un rôle.
 */
@RestController
@RequestMapping("/api/permissions")
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class PermissionController {

    @GetMapping
    public ResponseEntity<List<PermissionGroup>> getCatalogue() {
        Map<String, List<PermissionInfo>> parModule = new LinkedHashMap<>();
        for (PermissionEnum permission : PermissionEnum.values()) {
            parModule.computeIfAbsent(permission.getModule(), m -> new ArrayList<>())
                    .add(PermissionInfo.builder()
                            .name(permission.name())
                            .module(permission.getModule())
                            .ressource(permission.getRessource())
                            .libelle(permission.getLibelle())
                            .build());
        }

        List<PermissionGroup> catalogue = new ArrayList<>();
        parModule.forEach((module, permissions) ->
                catalogue.add(PermissionGroup.builder()
                        .module(module)
                        .permissions(permissions)
                        .build()));
        return ResponseEntity.ok(catalogue);
    }
}
