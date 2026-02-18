package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "DEPENSES_MISSION")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"mission", "typeDepense"})
@EqualsAndHashCode(callSuper = true, exclude = {"mission", "typeDepense"})
public class DepenseMissionEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String libelle;

	private Long montant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id", nullable = false)
	private MissionEntity mission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_depense_id", nullable = false)
	private TypeDepenseEntity typeDepense;
}
