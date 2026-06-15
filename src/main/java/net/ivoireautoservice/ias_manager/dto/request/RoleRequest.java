package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import net.ivoireautoservice.ias_manager.auth.PermissionEnum;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleRequest {

    @NotBlank(message = "Le nom du rôle est obligatoire")
    private String nom;

    private String libelle;

    private String description;

    @Builder.Default
    private Set<PermissionEnum> permissions = new HashSet<>();
}
