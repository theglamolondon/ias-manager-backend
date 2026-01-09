package net.ivoireautoservice.ias_manager.auth;

import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private UserRepository userRepository;

    public UtilisateurDetailsService(@Autowired UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Utilisateur loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email : " + username));
    }
}
