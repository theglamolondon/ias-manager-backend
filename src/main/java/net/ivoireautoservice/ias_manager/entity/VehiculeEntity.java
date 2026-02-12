package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;

import java.time.LocalDate;

@Entity
@Table(name = "VEHICULES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"type", "marque", "typeCarburant", "photoAvant", "photoArriere", "photoCoteDroit", "photoCoteGauche"})
@EqualsAndHashCode(exclude = {"type", "marque", "typeCarburant", "photoAvant", "photoArriere", "photoCoteDroit", "photoCoteGauche"})
public class VehiculeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String immatriculation;
	private LocalDate dateImmatriculation;
	@Column(name = "num_chassis")
	private String numChassis;
	private String couleur;

	private LocalDate dateAchat;
	private Long coutAchat;
	private String carteGrise;
	private String typeCommercial;
	private Integer nombrePlaces;
	private Long energie;
	private Long puissance;
	private Long kilometrage;
	private LocalDate finValiditeVisite;
	private LocalDate finValiditeAssurance;
	private LocalDate dateMiseCirculation;
	private LocalDate dateFinGarantie;
	private String concessionnaire;
	private Boolean isReforme;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private VehiculeStatusEnum statut = VehiculeStatusEnum.DISPONIBLE;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_id", referencedColumnName = "id", nullable = false)
	private TypeVehiculeEntity type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "marque_id")
	private MarqueEntity marque;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type_carburant_id")
	private TypeCarburantEntity typeCarburant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "photo_avant_id")
	private MediaEntity photoAvant;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "photo_arriere_id")
	private MediaEntity photoArriere;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "photo_cote_droit_id")
	private MediaEntity photoCoteDroit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "photo_cote_gauche_id")
	private MediaEntity photoCoteGauche;
}
