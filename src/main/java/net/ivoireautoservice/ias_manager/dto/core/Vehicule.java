package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Vehicule {
	private Long id;
	private String immatriculation;
	private LocalDate dateImmatriculation;
	private String numChassis;
	private String couleur;
	private LocalDate dateAchat;
	private Long coutAchat;
	private Long coutAssurance;
	private String carteGrise;
	private String typeCommercial;
	private Integer nombrePlaces;
	private String puissanceFiscale;
	private Long kilometrage;
	private LocalDate finValiditeVisite;
	private LocalDate finValiditeAssurance;
	private LocalDate dateMiseCirculation;
	private LocalDate dateFinGarantie;
	private String concessionnaire;
	private VehiculeStatusEnum statut;
	private Marque marque;
	private TypeVehicule type;
	private TypeCarburant energie;
	private TypeAssurance typeAssurance;
	private Assurance assurance;
	private Media photoAvant;
	private Media photoArriere;
	private Media photoCoteDroit;
	private Media photoCoteGauche;
}
