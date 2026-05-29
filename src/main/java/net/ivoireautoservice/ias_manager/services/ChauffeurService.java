package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Chauffeur;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.ChauffeurRequest;
import net.ivoireautoservice.ias_manager.entity.ChauffeurEntity;
import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import net.ivoireautoservice.ias_manager.enums.StatutChauffeurEnum;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.ChauffeurMapper;
import net.ivoireautoservice.ias_manager.repository.ChauffeurRepository;
import net.ivoireautoservice.ias_manager.repository.EmployeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChauffeurService {

    private final ChauffeurRepository chauffeurRepository;
    private final EmployeRepository employeRepository;
    private final ChauffeurMapper chauffeurMapper;

    @Transactional(readOnly = true)
    public List<Chauffeur> getAllChauffeurs() {
        return chauffeurMapper.toDtoList(chauffeurRepository.findAll());
    }

    @Transactional(readOnly = true)
    public PagedResponse<Chauffeur> getAllChauffeurs(String keyword, StatutChauffeurEnum statut, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasStatut = statut != null;

        Page<ChauffeurEntity> page;
        if (hasKeyword && hasStatut) {
            page = chauffeurRepository.searchByKeywordAndStatut(keyword.trim(), statut, pageable);
        } else if (hasKeyword) {
            page = chauffeurRepository.searchByKeyword(keyword.trim(), pageable);
        } else if (hasStatut) {
            page = chauffeurRepository.findByStatut(statut, pageable);
        } else {
            page = chauffeurRepository.findAll(pageable);
        }
        return PagedResponse.of(page.map(chauffeurMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Chauffeur getChauffeurById(Long id) {
        ChauffeurEntity entity = chauffeurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chauffeur", id));
        return chauffeurMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Chauffeur getChauffeurByEmployeId(Long employeId) {
        ChauffeurEntity entity = chauffeurRepository.findByEmployeId(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Chauffeur avec employé id " + employeId + " non trouvé"));
        return chauffeurMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Chauffeur getChauffeurByNumeroPermis(String numeroPermis) {
        ChauffeurEntity entity = chauffeurRepository.findByNumeroPermis(numeroPermis)
                .orElseThrow(() -> new ResourceNotFoundException("Chauffeur avec permis " + numeroPermis + " non trouvé"));
        return chauffeurMapper.toDto(entity);
    }

    @Transactional
    public Chauffeur createChauffeur(ChauffeurRequest request) {
        ChauffeurEntity entity = chauffeurMapper.toEntity(request);
        entity.setStatut(StatutChauffeurEnum.DISPONIBLE);
        resolveEmploye(request, entity);

        ChauffeurEntity saved = chauffeurRepository.save(entity);
        return chauffeurMapper.toDto(saved);
    }

    @Transactional
    public Chauffeur updateChauffeur(Long id, ChauffeurRequest request) {
        ChauffeurEntity entity = chauffeurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chauffeur", id));

        chauffeurMapper.updateEntity(request, entity);
        resolveEmploye(request, entity);

        ChauffeurEntity saved = chauffeurRepository.save(entity);
        return chauffeurMapper.toDto(saved);
    }

    @Transactional
    public Chauffeur changerStatut(Long id, StatutChauffeurEnum statut) {
        ChauffeurEntity entity = chauffeurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chauffeur", id));
        entity.setStatut(statut);
        return chauffeurMapper.toDto(chauffeurRepository.save(entity));
    }

    @Transactional
    public void deleteChauffeur(Long id) {
        if (!chauffeurRepository.existsById(id)) {
            throw new ResourceNotFoundException("Chauffeur", id);
        }
        chauffeurRepository.deleteById(id);
    }

    private void resolveEmploye(ChauffeurRequest request, ChauffeurEntity entity) {
        if (request.getEmployeId() != null) {
            EmployeEntity employe = employeRepository.findById(request.getEmployeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employé", request.getEmployeId()));
            entity.setEmploye(employe);
        } else {
            entity.setEmploye(null);
        }
    }
}
