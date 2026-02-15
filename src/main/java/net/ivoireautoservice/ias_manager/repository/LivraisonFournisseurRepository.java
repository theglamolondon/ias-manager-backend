package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.LivraisonFournisseurEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LivraisonFournisseurRepository extends JpaRepository<LivraisonFournisseurEntity, Long> {

    Optional<LivraisonFournisseurEntity> findByNumero(String numero);
    Optional<LivraisonFournisseurEntity> findByFactureId(Long factureId);
}
