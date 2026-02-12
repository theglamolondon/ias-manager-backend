package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.UtilisateurDto;
import net.ivoireautoservice.ias_manager.dto.request.UtilisateurRequest;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.UtilisateurMapper;
import net.ivoireautoservice.ias_manager.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UserRepository userRepository;
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
        Utilisateur entity = utilisateurMapper.toEntity(request);
        entity.setPassword(passwordEncoder.encode(request.getPassword()));
        Utilisateur saved = userRepository.save(entity);
        return utilisateurMapper.toDto(saved);
    }

    @Transactional
    public UtilisateurDto updateUtilisateur(Long id, UtilisateurRequest request) {
        Utilisateur entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        utilisateurMapper.updateEntity(request, entity);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            entity.setPassword(passwordEncoder.encode(request.getPassword()));
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
}