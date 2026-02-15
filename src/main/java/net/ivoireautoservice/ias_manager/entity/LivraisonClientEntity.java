package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "LIVRAISONS_CLIENT")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LivraisonClientEntity extends BaseLivraisonEntity {
}
