package net.ivoireautoservice.ias_manager.config;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.entity.TypeDepenseEntity;
import net.ivoireautoservice.ias_manager.repository.TypeDepenseRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Amorce les données de référence au démarrage (idempotent).
 * Crée les entrées uniquement si elles n'existent pas déjà (contrôle par libellé exact).
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class ReferenceDataSeeder implements ApplicationRunner {

    private static final List<String> TYPES_DEPENSE_SYSTEME = List.of(
            // Frais de route, engagés pendant une mission
            "Carburant",
            "Péage",
            "Réparation",
            "Hébergement",
            "Restauration",
            "Perdiem chauffeur",
            // Frais portés par le véhicule hors mission
            "Assurance",
            "Visite technique",
            "Pièces détachées",
            "Pneumatiques",
            "Lavage",
            "Gardiennage",
            "Amende",
            "Frais administratifs",
            "Autre"
    );

    private final TypeDepenseRepository typeDepenseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> existants = typeDepenseRepository.findAll()
                .stream()
                .map(t -> t.getLibelle())
                .filter(l -> l != null)
                .toList();

        for (String libelle : TYPES_DEPENSE_SYSTEME) {
            if (!existants.contains(libelle)) {
                typeDepenseRepository.save(TypeDepenseEntity.builder().libelle(libelle).build());
            }
        }
    }
}
