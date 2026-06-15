package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupeRequest {

    @NotBlank(message = "Le nom du groupe est obligatoire")
    private String nom;

    private String description;

    @Builder.Default
    private Set<Long> roleIds = new HashSet<>();
}
