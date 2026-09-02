package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "documents_vehicule")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"vehicule", "media"})
@EqualsAndHashCode(callSuper = true, exclude = {"vehicule", "media"})
public class DocumentVehiculeEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String label;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicule_id", nullable = false)
	private VehiculeEntity vehicule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "media_id", nullable = false)
	private MediaEntity media;
}
