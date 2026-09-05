package net.ivoireautoservice.ias_manager.config;

/**
 * Normalisation des critères de recherche textuels partagés entre la liste des
 * factures et ses KPI. Les deux doivent appliquer strictement les mêmes
 * prédicats, donc traiter les entrées vides à l'identique.
 */
public final class CritereRecherche {

	private CritereRecherche() {
	}

	/**
	 * Ramène à {@code null} un critère absent, vide ou composé uniquement
	 * d'espaces, et supprime les espaces superflus autour des autres. Les
	 * requêtes interprètent {@code null} comme « aucun filtre ».
	 */
	public static String normaliser(String critere) {
		return (critere != null && !critere.isBlank()) ? critere.trim() : null;
	}
}
