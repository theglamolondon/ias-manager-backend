package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.Role;
import net.ivoireautoservice.ias_manager.dto.request.RoleRequest;
import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.RoleMapper;
import net.ivoireautoservice.ias_manager.repository.RoleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleMapper.toDtoList(roleRepository.findAll());
    }

    @Transactional(readOnly = true)
    public PagedResponse<Role> getAllRoles(Pageable pageable) {
        return PagedResponse.of(roleRepository.findAll(pageable).map(roleMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleMapper.toDto(findOrThrow(id));
    }

    @Transactional
    public Role createRole(RoleRequest request) {
        String nom = normaliserNom(request.getNom());
        if (roleRepository.existsByNom(nom)) {
            throw new BadRequestException("Un rôle nommé '" + nom + "' existe déjà");
        }
        RoleEntity entity = roleMapper.toEntity(request);
        entity.setNom(nom);
        entity.setSystemRole(false);
        entity.setPermissions(safePermissions(request));
        return roleMapper.toDto(roleRepository.save(entity));
    }

    @Transactional
    public Role updateRole(Long id, RoleRequest request) {
        RoleEntity entity = findOrThrow(id);
        String nom = normaliserNom(request.getNom());
        boolean estRoleSysteme = Boolean.TRUE.equals(entity.getSystemRole());

        if (estRoleSysteme && !entity.getNom().equals(nom)) {
            throw new BadRequestException("Le nom d'un rôle système ne peut pas être modifié");
        }
        if (!entity.getNom().equals(nom) && roleRepository.existsByNom(nom)) {
            throw new BadRequestException("Un rôle nommé '" + nom + "' existe déjà");
        }

        // S3 : les permissions d'un rôle système sont figées (socle d'accès stable,
        // protection contre l'escalade de privilèges via ROLE_MANAGE). Seuls le libellé
        // et la description restent modifiables.
        if (estRoleSysteme && !safePermissions(request).equals(entity.getPermissions())) {
            throw new BadRequestException("Les permissions d'un rôle système ne peuvent pas être modifiées");
        }

        roleMapper.updateEntity(request, entity);
        entity.setNom(nom);
        if (!estRoleSysteme) {
            entity.setPermissions(safePermissions(request));
        }
        return roleMapper.toDto(roleRepository.save(entity));
    }

    @Transactional
    public void deleteRole(Long id) {
        RoleEntity entity = findOrThrow(id);
        if (Boolean.TRUE.equals(entity.getSystemRole())) {
            throw new BadRequestException("Un rôle système ne peut pas être supprimé");
        }
        roleRepository.delete(entity);
    }

    private RoleEntity findOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", id));
    }

    private Set<net.ivoireautoservice.ias_manager.auth.PermissionEnum> safePermissions(RoleRequest request) {
        return request.getPermissions() == null ? new HashSet<>() : new HashSet<>(request.getPermissions());
    }

    private String normaliserNom(String nom) {
        return nom == null ? null : nom.trim();
    }
}
