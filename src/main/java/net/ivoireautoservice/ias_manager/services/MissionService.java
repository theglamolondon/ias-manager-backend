package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.DepenseMission;
import net.ivoireautoservice.ias_manager.dto.core.Media;
import net.ivoireautoservice.ias_manager.dto.core.Mission;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.request.DepenseMissionRequest;
import net.ivoireautoservice.ias_manager.dto.request.MissionRequest;
import net.ivoireautoservice.ias_manager.entity.*;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.DepenseMissionMapper;
import net.ivoireautoservice.ias_manager.mapper.MediaMapper;
import net.ivoireautoservice.ias_manager.mapper.MissionMapper;
import net.ivoireautoservice.ias_manager.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

	private final MissionRepository missionRepository;
	private final DepenseMissionRepository depenseMissionRepository;
	private final PhotoMissionRepository photoMissionRepository;
	private final VehiculeRepository vehiculeRepository;
	private final ChauffeurRepository chauffeurRepository;
	private final TypeDepenseRepository typeDepenseRepository;
	private final MissionMapper missionMapper;
	private final DepenseMissionMapper depenseMissionMapper;
	private final MediaMapper mediaMapper;
	private final MediaService mediaService;

	// ==================== MISSIONS ====================

	@Transactional(readOnly = true)
	public PagedResponse<Mission> getAllMissions(Pageable pageable) {
		Page<Mission> page = missionRepository.findAll(pageable)
				.map(missionMapper::toDto);
		return PagedResponse.of(page);
	}

	@Transactional(readOnly = true)
	public Mission getMissionById(Long id) {
		MissionEntity entity = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));
		Mission dto = missionMapper.toDto(entity);

		List<DepenseMissionEntity> depenses = depenseMissionRepository.findByMissionId(id);
		dto.setDepenses(depenseMissionMapper.toDtoList(depenses));

		List<PhotoMissionEntity> photos = photoMissionRepository.findByMissionId(id);
		List<Media> medias = photos.stream()
				.map(photo -> mediaMapper.toDto(photo.getMedia()))
				.toList();
		dto.setMedias(medias);

		return dto;
	}

	@Transactional
	public Mission createMission(MissionRequest request) {
		VehiculeEntity vehicule = vehiculeRepository.findById(request.getVehiculeId())
				.orElseThrow(() -> new ResourceNotFoundException("Véhicule", request.getVehiculeId()));

		MissionEntity entity = missionMapper.toEntity(request);
		entity.setVehicule(vehicule);

		if (Boolean.TRUE.equals(request.getWithChauffeur()) && request.getChauffeurId() != null) {
			ChauffeurEntity chauffeur = chauffeurRepository.findById(request.getChauffeurId())
					.orElseThrow(() -> new ResourceNotFoundException("Chauffeur", request.getChauffeurId()));
			entity.setChauffeur(chauffeur);
		}

		MissionEntity saved = missionRepository.save(entity);
		return missionMapper.toDto(saved);
	}

	@Transactional
	public Mission updateMission(Long id, MissionRequest request) {
		MissionEntity entity = missionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", id));

		VehiculeEntity vehicule = vehiculeRepository.findById(request.getVehiculeId())
				.orElseThrow(() -> new ResourceNotFoundException("Véhicule", request.getVehiculeId()));

		missionMapper.updateEntity(request, entity);
		entity.setVehicule(vehicule);

		if (Boolean.TRUE.equals(request.getWithChauffeur()) && request.getChauffeurId() != null) {
			ChauffeurEntity chauffeur = chauffeurRepository.findById(request.getChauffeurId())
					.orElseThrow(() -> new ResourceNotFoundException("Chauffeur", request.getChauffeurId()));
			entity.setChauffeur(chauffeur);
		} else {
			entity.setChauffeur(null);
		}

		MissionEntity saved = missionRepository.save(entity);
		return missionMapper.toDto(saved);
	}

	// ==================== DEPENSES MISSION ====================

	@Transactional
	public DepenseMission addDepense(Long missionId, DepenseMissionRequest request) {
		MissionEntity mission = missionRepository.findById(missionId)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", missionId));

		TypeDepenseEntity typeDepense = typeDepenseRepository.findById(request.getTypeDepenseId())
				.orElseThrow(() -> new ResourceNotFoundException("Type de dépense", request.getTypeDepenseId()));

		DepenseMissionEntity entity = depenseMissionMapper.toEntity(request);
		entity.setMission(mission);
		entity.setTypeDepense(typeDepense);

		DepenseMissionEntity saved = depenseMissionRepository.save(entity);
		return depenseMissionMapper.toDto(saved);
	}

	// ==================== PHOTOS MISSION ====================

	@Transactional
	public Media addPhoto(Long missionId, MultipartFile file) {
		MissionEntity mission = missionRepository.findById(missionId)
				.orElseThrow(() -> new ResourceNotFoundException("Mission", missionId));

		Media media = mediaService.uploadMedia(file);
		MediaEntity mediaEntity = mediaService.getMediaEntity(media.getId());

		PhotoMissionEntity photo = PhotoMissionEntity.builder()
				.mission(mission)
				.media(mediaEntity)
				.build();
		photoMissionRepository.save(photo);

		return media;
	}
}
