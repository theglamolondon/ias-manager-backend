package net.ivoireautoservice.ias_manager.repository;

import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByEmail(String email);
}
