package net.ivoireautoservice.ias_manager.event;

/**
 * Événement de domaine publié par {@code MissionService} après la création
 * d'une mission. Consommé (entre autres) par le module notification,
 * après commit de la transaction.
 */
public record MissionCreatedEvent(
		Long missionId,
		String codeMission,
		String immatriculation,
		String clientNom) {
}
