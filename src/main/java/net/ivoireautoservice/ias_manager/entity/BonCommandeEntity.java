package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "BONS_COMMANDE")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "partenaire")
@EqualsAndHashCode(exclude = "partenaire")
public class BonCommandeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDate dateCommande;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partenaire_id", referencedColumnName = "id", nullable = false)
	private PartenaireEntity partenaire;
}
