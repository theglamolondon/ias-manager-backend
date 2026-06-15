package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.UtilisateurDto;
import net.ivoireautoservice.ias_manager.dto.request.UtilisateurRequest;
import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import net.ivoireautoservice.ias_manager.entity.GroupeEntity;
import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.UtilisateurMapper;
import net.ivoireautoservice.ias_manager.repository.EmployeRepository;
import net.ivoireautoservice.ias_manager.repository.GroupeRepository;
import net.ivoireautoservice.ias_manager.repository.RoleRepository;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UserRepository userRepository;
    private final EmployeRepository employeRepository;
    private final RoleRepository roleRepository;
    private final GroupeRepository groupeRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PagedResponse<UtilisateurDto> getAllUtilisateurs(Pageable pageable) {
        return PagedResponse.of(userRepository.findAll(pageable).map(utilisateurMapper::toDto));
    }

    @Transactional(readOnly = true)
    public UtilisateurDto getUtilisateurById(Long id) {
        Utilisateur entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        return utilisateurMapper.toDto(entity);
    }

    @Transactional
    public UtilisateurDto createUtilisateur(UtilisateurRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Le mot de passe est obligatoire à la création");
        }
        Utilisateur entity = utilisateurMapper.toEntity(request);
        entity.setPassword(passwordEncoder.encode(request.getPassword()));
        entity.setEmploye(resolveEmploye(request.getEmployeId()));
        // Tout nouvel utilisateur doit changer son mot de passe à la 1ère connexion.
        entity.setHasChangePassword(false);
        Utilisateur saved = userRepository.save(entity);
        return utilisateurMapper.toDto(saved);
    }

    @Transactional
    public UtilisateurDto updateUtilisateur(Long id, UtilisateurRequest request) {
        Utilisateur entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        utilisateurMapper.updateEntity(request, entity);
        if (request.getEmployeId() != null) {
            entity.setEmploye(resolveEmploye(request.getEmployeId()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            entity.setPassword(passwordEncoder.encode(request.getPassword()));
            // Un mot de passe défini par un admin via le module utilisateurs
            // est considéré comme un mot de passe initial / réinitialisé.
            entity.setHasChangePassword(false);
        }
        Utilisateur saved = userRepository.save(entity);
        return utilisateurMapper.toDto(saved);
    }

    @Transactional
    public void deleteUtilisateur(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur", id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Appelé par l'utilisateur connecté lui-même pour définir/changer son
     * mot de passe. Lève automatiquement le flag hasChangePassword.
     */
    @Transactional
    public UtilisateurDto changePasswordSelf(Utilisateur connected, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new BadRequestException("Le nouveau mot de passe est obligatoire");
        }
        if (newPassword.length() < 6) {
            throw new BadRequestException("Le mot de passe doit contenir au moins 6 caractères");
        }
        Utilisateur entity = userRepository.findById(connected.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", connected.getId()));
        entity.setPassword(passwordEncoder.encode(newPassword));
        entity.setHasChangePassword(true);
        return utilisateurMapper.toDto(userRepository.save(entity));
    }

    // ------------------------------------------------------------------
    // RBAC : attribution des rôles directs et des groupes
    // ------------------------------------------------------------------

    /** Remplace l'ensemble des rôles attribués directement à l'utilisateur. */
    @Transactional
    public UtilisateurDto assignRoles(Long userId, Set<Long> roleIds) {
        Utilisateur user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
        Set<RoleEntity> roles = new HashSet<>();
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                roles.add(roleRepository.findById(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Rôle", roleId)));
            }
        }
        user.setRoles(roles);
        return utilisateurMapper.toDto(userRepository.save(user));
    }

    /** Remplace l'ensemble des groupes auxquels l'utilisateur appartient. */
    @Transactional
    public UtilisateurDto assignGroupes(Long userId, Set<Long> groupeIds) {
        Utilisateur user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
        Set<GroupeEntity> groupes = new HashSet<>();
        if (groupeIds != null) {
            for (Long groupeId : groupeIds) {
                groupes.add(groupeRepository.findById(groupeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupeId)));
            }
        }
        user.setGroupes(groupes);
        return utilisateurMapper.toDto(userRepository.save(user));
    }

    private EmployeEntity resolveEmploye(Long employeId) {
        if (employeId == null) return null;
        return employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));
    }
}
