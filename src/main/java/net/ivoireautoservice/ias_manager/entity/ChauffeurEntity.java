package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "CHAUFFEURS")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "employe")
@EqualsAndHashCode(exclude = "employe")
public class ChauffeurEntity {

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
}
