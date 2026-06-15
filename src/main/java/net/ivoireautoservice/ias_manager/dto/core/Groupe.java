package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Groupe {
    private Long id;
    private String nom;
    private String description;
    private List<Role> roles;
}
