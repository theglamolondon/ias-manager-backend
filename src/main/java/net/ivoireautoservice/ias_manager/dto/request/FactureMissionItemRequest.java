package net.ivoireautoservice.ias_manager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;


/**
 * Item d'une facture mission groupée : une mission non encore facturée
 * à inclure dans la facture, avec ses coûts de location ajustables au
 * moment de la facturation. Compatible avec tous les types de tarification :
 * l'unité de la durée (jours ou mois) et la sémantique du tarif (unitaire
 * ou forfaitaire) sont déduites du typeTarification de la mission côté
 * service.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FactureMissionItemRequest {

	@NotNull(message = "L'identifiant de la mission est requis")
	private Long missionId;

	/**
	 * Tarif de la mission, modifiable à la facturation.
	 * Sémantique selon le typeTarification de la mission :
	 *   - JOURNALIERE : tarif journalier
	 *   - MENSUELLE / INDEFINIE : tarif mensuel
	 *   - UNIQUE : montant forfaitaire (la durée est ignorée et forcée à 1)
	 */
	@NotNull(message = "Le tarif est requis")
	private BigDecimal tarif;

	/**
	 * Nombre d'unités facturées (jours ou mois selon typeTarification).
	 * Ignoré pour UNIQUE (forcé à 1 côté service).
	 */
	@NotNull(message = "La durée de location est requise")
	private Long dureeLocation;

}
