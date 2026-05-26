package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Employe;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.EmployeRequest;
import net.ivoireautoservice.ias_manager.entity.ChauffeurEntity;
import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import net.ivoireautoservice.ias_manager.entity.ServiceEntity;
import net.ivoireautoservice.ias_manager.exception.BadRequestException;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.EmployeMapper;
import net.ivoireautoservice.ias_manager.repository.ChauffeurRepository;
import net.ivoireautoservice.ias_manager.repository.EmployeRepository;
import net.ivoireautoservice.ias_manager.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final ServiceRepository serviceRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final EmployeMapper employeMapper;

    @Transactional(readOnly = true)
    public List<Employe> getAllEmployes() {
        List<EmployeEntity> entities = employeRepository.findAll();
        Map<Long, ChauffeurEntity> chauffeurs = chauffeursByEmployeId(entities);
        return entities.stream().map(e -> toDtoWithChauffeur(e, chauffeurs.get(e.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<Employe> getAllEmployes(String keyword, Pageable pageable) {
        Page<EmployeEntity> page = (keyword != null && !keyword.isBlank())
                ? employeRepository.searchByKeyword(keyword.trim(), pageable)
                : employeRepository.findAll(pageable);
        Map<Long, ChauffeurEntity> chauffeurs = chauffeursByEmployeId(page.getContent());
        return PagedResponse.of(page.map(e -> toDtoWithChauffeur(e, chauffeurs.get(e.getId()))));
    }

    @Transactional(readOnly = true)
    public Employe getEmployeById(Long id) {
        EmployeEntity entity = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));
        return toDtoWithChauffeur(entity, chauffeurRepository.findByEmployeId(id).orElse(null));
    }

    @Transactional(readOnly = true)
    public Employe getEmployeByMatricule(String matricule) {
        EmployeEntity entity = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Employé avec matricule " + matricule + " non trouvé"));
        return toDtoWithChauffeur(entity, chauffeurRepository.findByEmployeId(entity.getId()).orElse(null));
    }

    @Transactional
    public Employe createEmploye(EmployeRequest request) {
        EmployeEntity entity = employeMapper.toEntity(request);
        resolveService(request, entity);
        EmployeEntity saved = employeRepository.save(entity);
        ChauffeurEntity chauffeur = syncChauffeur(saved, request);
        return toDtoWithChauffeur(saved, chauffeur);
    }

    @Transactional
    public Employe updateEmploye(Long id, EmployeRequest request) {
        EmployeEntity entity = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        employeMapper.updateEntity(request, entity);
        resolveService(request, entity);
        EmployeEntity saved = employeRepository.save(entity);
        ChauffeurEntity chauffeur = syncChauffeur(saved, request);
        return toDtoWithChauffeur(saved, chauffeur);
    }

    @Transactional
    public void deleteEmploye(Long id) {
        if (!employeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employé", id);
        }
        chauffeurRepository.findByEmployeId(id).ifPresent(chauffeurRepository::delete);
        employeRepository.deleteById(id);
    }

    private void resolveService(EmployeRequest request, EmployeEntity entity) {
        if (request.getServiceId() != null) {
            ServiceEntity service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", request.getServiceId()));
            entity.setService(service);
        } else {
            entity.setService(null);
        }
    }

    private ChauffeurEntity syncChauffeur(EmployeEntity employe, EmployeRequest request) {
        ChauffeurEntity existing = chauffeurRepository.findByEmployeId(employe.getId()).orElse(null);
        boolean wantsChauffeur = Boolean.TRUE.equals(request.getIsChauffeur());

        if (!wantsChauffeur) {
            if (existing != null) {
                chauffeurRepository.delete(existing);
            }
            return null;
        }

        if (request.getNumeroPermis() == null || request.getNumeroPermis().isBlank()) {
            throw new BadRequestException("Le numéro de permis est obligatoire pour un chauffeur");
        }
        if (request.getTypePermis() == null || request.getTypePermis().isBlank()) {
            throw new BadRequestException("Le type de permis est obligatoire pour un chauffeur");
        }

        ChauffeurEntity chauffeur = existing != null ? existing : ChauffeurEntity.builder().employe(employe).build();
        chauffeur.setEmploye(employe);
        chauffeur.setNumeroPermis(request.getNumeroPermis());
        chauffeur.setExpDatePermis(request.getExpDatePermis());
        chauffeur.setTypePermis(request.getTypePermis());
        return chauffeurRepository.save(chauffeur);
    }

    private Map<Long, ChauffeurEntity> chauffeursByEmployeId(List<EmployeEntity> employes) {
        if (employes.isEmpty()) return Map.of();
        Set<Long> ids = employes.stream().map(EmployeEntity::getId).collect(Collectors.toSet());
        return chauffeurRepository.findAll().stream()
                .filter(c -> c.getEmploye() != null && ids.contains(c.getEmploye().getId()))
                .collect(Collectors.toMap(c -> c.getEmploye().getId(), c -> c, (a, b) -> a));
    }

    private Employe toDtoWithChauffeur(EmployeEntity entity, ChauffeurEntity chauffeur) {
        Employe dto = employeMapper.toDto(entity);
        if (chauffeur != null) {
            dto.setIsChauffeur(true);
            dto.setChauffeurId(chauffeur.getId());
            dto.setNumeroPermis(chauffeur.getNumeroPermis());
            dto.setExpDatePermis(chauffeur.getExpDatePermis());
            dto.setTypePermis(chauffeur.getTypePermis());
        } else {
            dto.setIsChauffeur(false);
        }
        return dto;
    }
}
