package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.FactureStatusEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "FACTURES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"partenaire"})
@EqualsAndHashCode(exclude = {"partenaire"})
public class FactureEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String numProforma;

	private LocalDateTime dhmsCreationPiece;

	private String numFacture;

	private LocalDateTime dhmsFacture;

	private Long montantHt;

	private Float tva;

	private Long montantTtc;

	private LocalDate delaiLivraison;

	private LocalDate validite;

	private String objet;

	@Enumerated(EnumType.ORDINAL)
	private FactureStatusEnum statut;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partenaire_id", referencedColumnName = "id")
	private PartenaireEntity partenaire;
}