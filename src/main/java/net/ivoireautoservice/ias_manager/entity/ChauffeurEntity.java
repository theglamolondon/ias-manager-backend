package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.enums.StatutChauffeurEnum;

import java.time.LocalDate;

@Entity
@Table(name = "CHAUFFEURS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = "employe")
@EqualsAndHashCode(callSuper = true, exclude = "employe")
public class ChauffeurEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employe_id")
	private EmployeEntity employe;

	@Column(nullable = false, unique = true)
	private String numeroPermis;

	private LocalDate expDatePermis;
	private String typePermis;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private StatutChauffeurEnum statut = StatutChauffeurEnum.DISPONIBLE;
}
