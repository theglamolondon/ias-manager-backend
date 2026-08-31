package net.ivoireautoservice.ias_manager.services;

import net.ivoireautoservice.ias_manager.auth.PermissionEnum;
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

	/**
	 * Vrai si l'utilisateur courant détient la permission donnée. Complète les
	 * {@code @PreAuthorize} pour les cas où l'autorisation ne fait pas
	 * qu'autoriser/interdire mais <b>module</b> le résultat (ex. périmètre des
	 * comptes de trésorerie : tous vs. uniquement ceux qui lui sont rattachés).
	 */
	public boolean hasAuthority(PermissionEnum permission) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}
		return authentication.getAuthorities().stream()
				.anyMatch(a -> permission.name().equals(a.getAuthority()));
	}

	public Utilisateur getUtilisateurConnecteOrNull() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Utilisateur)) {
			return null;
		}
		return (Utilisateur) authentication.getPrincipal();
	}
}
