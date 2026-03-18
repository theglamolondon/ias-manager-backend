package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ivoireautoservice.ias_manager.enums.VehiculeStatusEnum;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VehiculeRequest {

	@NotBlank(message = "L'immatriculation est obligatoire")
	private String immatriculation;

	private LocalDate dateImmatriculation;
//	private String numChassis;
	private String couleur;
	private LocalDate dateAchat;
	private Long coutAchat;
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

	@Builder.Default
	private VehiculeStatusEnum statut = VehiculeStatusEnum.DISPONIBLE;

	private String marque;

	@NotNull(message = "Le type de véhicule est obligatoire")
	private Long typeId;

	private Long energieId;
	private Long typeAssuranceId;
	private Long assuranceId;

	private String photoAvantId;
	private String photoArriereId;
	private String photoCoteDroitId;
	private String photoCoteGaucheId;
}
