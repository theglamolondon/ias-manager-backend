package net.ivoireautoservice.ias_manager.enums.notification;

/**
 * Types de notifications, utilisés par le frontend pour choisir l'icône/couleur
 * et par les clés de dédoublonnage.
 */
public enum TypeNotificationEnum {
	/** Une mission vient d'être créée. */
	MISSION_CREEE,
	/** Un document administratif d'un véhicule arrive à expiration (ou est expiré). */
	DOCUMENT_EXPIRATION,
	/** Notification informative générique. */
	INFO
}
