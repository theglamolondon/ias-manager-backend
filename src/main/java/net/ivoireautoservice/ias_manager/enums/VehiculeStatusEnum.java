package net.ivoireautoservice.ias_manager.enums;

public enum VehiculeStatusEnum {
	/** Véhicule opérationnel, prêt à partir en mission. */
	DISPONIBLE,
	/** Véhicule actuellement en mission. */
	MISSION,
	/** Véhicule immobilisé au garage pour maintenance ou réparation. */
	GARAGE,
	/** Véhicule avec dommages irréparables — perte définitive pour l'entreprise. */
	SINISTRE,
	/** Véhicule sorti de la flotte définitivement (réforme administrative). */
	REFORME,
	/** Véhicule non opérationnel pour défaut de pièces administratives (papiers). */
	INDISPONIBLE
}
