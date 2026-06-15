package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Groupe;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.GroupeRequest;
import net.ivoireautoservice.ias_manager.entity.GroupeEntity;
import net.ivoireautoservice.ias_manager.entity.RoleEntity;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.GroupeMapper;
import net.ivoireautoservice.ias_manager.repository.GroupeRepository;
import net.ivoireautoservice.ias_manager.repository.RoleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupeService {

    private final GroupeRepository groupeRepository;
    private final RoleRepository roleRepository;
    private final GroupeMapper groupeMapper;

    @Transactional(readOnly = true)
    public List<Groupe> getAllGroupes() {
        return groupeMapper.toDtoList(groupeRepository.findAll());
    }

    @Transactional(readOnly = true)
    public PagedResponse<Groupe> getAllGroupes(Pageable pageable) {
        return PagedResponse.of(groupeRepository.findAll(pageable).map(groupeMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Groupe getGroupeById(Long id) {
        return groupeMapper.toDto(findOrThrow(id));
    }

    @Transactional
    public Groupe createGroupe(GroupeRequest request) {
        String nom = normaliserNom(request.getNom());
        if (groupeRepository.existsByNom(nom)) {
            throw new BadRequestException("Un groupe nommé '" + nom + "' existe déjà");
        }
        GroupeEntity entity = groupeMapper.toEntity(request);
        entity.setNom(nom);
        entity.setRoles(resolveRoles(request.getRoleIds()));
        return groupeMapper.toDto(groupeRepository.save(entity));
    }

    @Transactional
    public Groupe updateGroupe(Long id, GroupeRequest request) {
        GroupeEntity entity = findOrThrow(id);
        String nom = normaliserNom(request.getNom());
        if (!entity.getNom().equals(nom) && groupeRepository.existsByNom(nom)) {
            throw new BadRequestException("Un groupe nommé '" + nom + "' existe déjà");
        }
        groupeMapper.updateEntity(request, entity);
        entity.setNom(nom);
        entity.setRoles(resolveRoles(request.getRoleIds()));
        return groupeMapper.toDto(groupeRepository.save(entity));
    }

    @Transactional
    public void deleteGroupe(Long id) {
        if (!groupeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Groupe", id);
        }
        groupeRepository.deleteById(id);
    }

    private GroupeEntity findOrThrow(Long id) {
        return groupeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe", id));
    }

    /** Résout des ids de rôles en entités, en échouant si l'un d'eux n'existe pas. */
    private Set<RoleEntity> resolveRoles(Set<Long> roleIds) {
        Set<RoleEntity> roles = new HashSet<>();
        if (roleIds == null || roleIds.isEmpty()) {
            return roles;
        }
        for (Long roleId : roleIds) {
            roles.add(roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle", roleId)));
        }
        return roles;
    }

    private String normaliserNom(String nom) {
        return nom == null ? null : nom.trim();
    }
}
