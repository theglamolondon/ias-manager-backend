package net.ivoireautoservice.ias_manager.enums;

public enum StatutChauffeurEnum {
    /** Chauffeur disponible, peut être affecté à une mission. */
    DISPONIBLE,
    /** Chauffeur actuellement en mission. */
    MISSION,
    /** Chauffeur temporairement indisponible (congé, maladie, etc.). */
    INDISPONIBLE
}
