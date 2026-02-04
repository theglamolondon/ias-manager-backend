package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DEPENSES_MISSION")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"mission", "typeDepense"})
@EqualsAndHashCode(exclude = {"mission", "typeDepense"})
public class DepenseMissionEntity {

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
