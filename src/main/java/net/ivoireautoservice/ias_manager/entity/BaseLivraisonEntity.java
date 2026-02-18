package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@ToString(exclude = {"facture"})
@EqualsAndHashCode(callSuper = true, exclude = {"facture"})
public abstract class BaseLivraisonEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDateTime dhmsLivraison;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "facture_id", referencedColumnName = "id", unique = true)
	private FactureEntity facture;
}
