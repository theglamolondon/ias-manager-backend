package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.Employe;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.EmployeRequest;
import net.ivoireautoservice.ias_manager.entity.EmployeEntity;
import net.ivoireautoservice.ias_manager.entity.ServiceEntity;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.EmployeMapper;
import net.ivoireautoservice.ias_manager.repository.EmployeRepository;
import net.ivoireautoservice.ias_manager.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final ServiceRepository serviceRepository;
    private final EmployeMapper employeMapper;

    @Transactional(readOnly = true)
    public List<Employe> getAllEmployes() {
        return employeMapper.toDtoList(employeRepository.findAll());
    }

    @Transactional(readOnly = true)
    public PagedResponse<Employe> getAllEmployes(String keyword, Pageable pageable) {
        Page<EmployeEntity> page = (keyword != null && !keyword.isBlank())
                ? employeRepository.searchByKeyword(keyword.trim(), pageable)
                : employeRepository.findAll(pageable);
        return PagedResponse.of(page.map(employeMapper::toDto));
    }

    @Transactional(readOnly = true)
    public Employe getEmployeById(Long id) {
        EmployeEntity entity = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));
        return employeMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Employe getEmployeByMatricule(String matricule) {
        EmployeEntity entity = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Employé avec matricule " + matricule + " non trouvé"));
        return employeMapper.toDto(entity);
    }

    @Transactional
    public Employe createEmploye(EmployeRequest request) {
        EmployeEntity entity = employeMapper.toEntity(request);
        resolveService(request, entity);
        EmployeEntity saved = employeRepository.save(entity);
        return employeMapper.toDto(saved);
    }

    @Transactional
    public Employe updateEmploye(Long id, EmployeRequest request) {
        EmployeEntity entity = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));

        employeMapper.updateEntity(request, entity);
        resolveService(request, entity);
        EmployeEntity saved = employeRepository.save(entity);
        return employeMapper.toDto(saved);
    }

    @Transactional
    public void deleteEmploye(Long id) {
        if (!employeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employé", id);
        }
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
}
