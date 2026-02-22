package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

	public Utilisateur getUtilisateurConnecte() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur)) {
			throw new IllegalStateException("Aucun utilisateur connecté");
		}
		return (Utilisateur) authentication.getPrincipal();
	}
}
