package net.ivoireautoservice.ias_manager.enums;

/**
 * Provenance d'une ligne de compte, et par extension porteur de sa valeur analytique.
 *
 * <p>Une même dépense ne doit être comptée qu'une fois dans les agrégats métier
 * (coût d'un véhicule, notamment). La règle est : <b>chaque dépense a un porteur
 * unique de sa valeur</b> — l'intervention porte son coût, la facture porte le sien,
 * la ligne saisie à la main porte le sien. Les agrégats somment les porteurs et
 * ignorent les lignes générées, sans quoi une intervention réglée serait comptée
 * deux fois (une fois via {@code interventions.cout}, une fois via son décaissement).</p>
 *
 * <p>Il n'existe pas de valeur {@code MISSION} : une dépense engagée pour une mission
 * est une saisie manuelle imputée à cette mission, donc {@link #MANUELLE}.</p>
 */
public enum LigneCompteOrigine {

	/** Saisie directe en trésorerie. Seule origine porteuse de valeur analytique. */
	MANUELLE,

	/** Générée par le règlement d'une intervention : la valeur est portée par l'intervention. */
	INTERVENTION,

	/** Générée par l'encaissement, le paiement ou le remboursement d'une facture. */
	FACTURE
}
