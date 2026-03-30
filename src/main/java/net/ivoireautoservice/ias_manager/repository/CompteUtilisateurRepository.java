package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.CompteUtilisateurEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompteUtilisateurRepository extends JpaRepository<CompteUtilisateurEntity, Long> {

	List<CompteUtilisateurEntity> findByCompteId(Long compteId);

	Optional<CompteUtilisateurEntity> findByCompteIdAndUtilisateurId(Long compteId, Long utilisateurId);

	void deleteByCompteId(Long compteId);

	List<CompteUtilisateurEntity> findByUtilisateurId(Long utilisateurId);

	List<CompteUtilisateurEntity> findByUtilisateurIdAndCanApproTrueAndCompteCanApproTrue(Long utilisateurId);
}
