package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "COMPTES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String intitule;

	@Column(nullable = false, unique = true)
	private String numero;

	private String description;

	@Column(nullable = false)
	private Long balance;

	@Column(name = "is_appro", nullable = false)
	private Boolean isAppro;
}
