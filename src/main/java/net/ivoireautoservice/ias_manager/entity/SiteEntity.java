package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SITES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SiteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "raison_sociale")
	private String raisonSociale;

	private String logo;

	private String devise;
}
