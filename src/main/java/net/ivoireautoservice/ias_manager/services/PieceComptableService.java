package net.ivoireautoservice.ias_manager.services;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.LignePieceComptable;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.PieceComptable;
import net.ivoireautoservice.ias_manager.dto.request.LignePieceComptableRequest;
import net.ivoireautoservice.ias_manager.dto.request.PieceComptableRequest;
import net.ivoireautoservice.ias_manager.entity.LignePieceComptableEntity;
import net.ivoireautoservice.ias_manager.entity.PartenaireEntity;
import net.ivoireautoservice.ias_manager.entity.PieceComptableEntity;
import net.ivoireautoservice.ias_manager.entity.ProduitEntity;
import net.ivoireautoservice.ias_manager.entity.TypeStatutPieceComptableEntity;
import net.ivoireautoservice.ias_manager.exception.ResourceNotFoundException;
import net.ivoireautoservice.ias_manager.mapper.LignePieceComptableMapper;
import net.ivoireautoservice.ias_manager.mapper.PieceComptableMapper;
import net.ivoireautoservice.ias_manager.repository.LignePieceComptableRepository;
import net.ivoireautoservice.ias_manager.repository.PartenaireRepository;
import net.ivoireautoservice.ias_manager.repository.PieceComptableRepository;
import net.ivoireautoservice.ias_manager.repository.ProduitRepository;
import net.ivoireautoservice.ias_manager.repository.TypeStatutPieceComptableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PieceComptableService {

	private final PieceComptableRepository pieceComptableRepository;
	private final LignePieceComptableRepository lignePieceComptableRepository;
	private final TypeStatutPieceComptableRepository typeStatutPieceComptableRepository;
	private final PartenaireRepository partenaireRepository;
	private final ProduitRepository produitRepository;
	private final PieceComptableMapper pieceComptableMapper;
	private final LignePieceComptableMapper lignePieceComptableMapper;

	// ==================== PIECES COMPTABLES ====================

	@Transactional(readOnly = true)
	public PagedResponse<PieceComptable> getAllPiecesComptables(Pageable pageable) {
		Page<PieceComptable> dtoPage = pieceComptableRepository.findAll(pageable)
				.map(pieceComptableMapper::toDto);
		return PagedResponse.of(dtoPage);
	}

	@Transactional(readOnly = true)
	public PieceComptable getPieceComptableById(Long id) {
		PieceComptableEntity entity = pieceComptableRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pièce comptable", id));
		return pieceComptableMapper.toDto(entity);
	}

	@Transactional(readOnly = true)
	public PieceComptable getPieceComptableByNumProforma(String numProforma) {
		PieceComptableEntity entity = pieceComptableRepository.findByNumProforma(numProforma)
				.orElseThrow(() -> new ResourceNotFoundException("Pièce comptable avec numéro proforma " + numProforma + " non trouvée"));
		return pieceComptableMapper.toDto(entity);
	}

	@Transactional(readOnly = true)
	public PieceComptable getPieceComptableByNumFacture(String numFacture) {
		PieceComptableEntity entity = pieceComptableRepository.findByNumFacture(numFacture)
				.orElseThrow(() -> new ResourceNotFoundException("Pièce comptable avec numéro facture " + numFacture + " non trouvée"));
		return pieceComptableMapper.toDto(entity);
	}

	@Transactional
	public PieceComptable createPieceComptable(PieceComptableRequest request) {
		TypeStatutPieceComptableEntity typeStatut = typeStatutPieceComptableRepository.findById(request.getTypeStatutId())
				.orElseThrow(() -> new ResourceNotFoundException("Type statut pièce comptable", request.getTypeStatutId()));

		PartenaireEntity partenaire = partenaireRepository.findById(request.getPartenaireId())
				.orElseThrow(() -> new ResourceNotFoundException("Partenaire", request.getPartenaireId()));

		PieceComptableEntity entity = pieceComptableMapper.toEntity(request);
		entity.setTypeStatut(typeStatut);
		entity.setPartenaire(partenaire);

		PieceComptableEntity saved = pieceComptableRepository.save(entity);
		return pieceComptableMapper.toDto(saved);
	}

	@Transactional
	public PieceComptable updatePieceComptable(Long id, PieceComptableRequest request) {
		PieceComptableEntity entity = pieceComptableRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pièce comptable", id));

		TypeStatutPieceComptableEntity typeStatut = typeStatutPieceComptableRepository.findById(request.getTypeStatutId())
				.orElseThrow(() -> new ResourceNotFoundException("Type statut pièce comptable", request.getTypeStatutId()));

		PartenaireEntity partenaire = partenaireRepository.findById(request.getPartenaireId())
				.orElseThrow(() -> new ResourceNotFoundException("Partenaire", request.getPartenaireId()));

		pieceComptableMapper.updateEntity(request, entity);
		entity.setTypeStatut(typeStatut);
		entity.setPartenaire(partenaire);

		PieceComptableEntity saved = pieceComptableRepository.save(entity);
		return pieceComptableMapper.toDto(saved);
	}

	@Transactional
	public void deletePieceComptable(Long id) {
		if (!pieceComptableRepository.existsById(id)) {
			throw new ResourceNotFoundException("Pièce comptable", id);
		}
		pieceComptableRepository.deleteById(id);
	}

	// ==================== LIGNES PIECE COMPTABLE ====================

	@Transactional(readOnly = true)
	public PagedResponse<LignePieceComptable> getLignesByPieceComptable(Long pieceComptableId, Pageable pageable) {
		if (!pieceComptableRepository.existsById(pieceComptableId)) {
			throw new ResourceNotFoundException("Pièce comptable", pieceComptableId);
		}
		Page<LignePieceComptable> dtoPage = lignePieceComptableRepository.findByPieceComptableId(pieceComptableId, pageable)
				.map(lignePieceComptableMapper::toDto);
		return PagedResponse.of(dtoPage);
	}

	@Transactional(readOnly = true)
	public LignePieceComptable getLigneById(Long pieceComptableId, Long ligneId) {
		if (!pieceComptableRepository.existsById(pieceComptableId)) {
			throw new ResourceNotFoundException("Pièce comptable", pieceComptableId);
		}
		LignePieceComptableEntity entity = lignePieceComptableRepository.findById(ligneId)
				.orElseThrow(() -> new ResourceNotFoundException("Ligne pièce comptable", ligneId));

		if (!entity.getPieceComptable().getId().equals(pieceComptableId)) {
			throw new ResourceNotFoundException("Ligne pièce comptable " + ligneId + " n'appartient pas à la pièce comptable " + pieceComptableId);
		}

		return lignePieceComptableMapper.toDto(entity);
	}

	@Transactional
	public LignePieceComptable createLigne(Long pieceComptableId, LignePieceComptableRequest request) {
		PieceComptableEntity pieceComptable = pieceComptableRepository.findById(pieceComptableId)
				.orElseThrow(() -> new ResourceNotFoundException("Pièce comptable", pieceComptableId));

		ProduitEntity produit = null;
		if (request.getProduitId() != null) {
			produit = produitRepository.findById(request.getProduitId())
					.orElseThrow(() -> new ResourceNotFoundException("Produit", request.getProduitId()));
		}

		LignePieceComptableEntity entity = lignePieceComptableMapper.toEntity(request);
		entity.setPieceComptable(pieceComptable);
		entity.setProduit(produit);

		LignePieceComptableEntity saved = lignePieceComptableRepository.save(entity);
		return lignePieceComptableMapper.toDto(saved);
	}

	@Transactional
	public LignePieceComptable updateLigne(Long pieceComptableId, Long ligneId, LignePieceComptableRequest request) {
		if (!pieceComptableRepository.existsById(pieceComptableId)) {
			throw new ResourceNotFoundException("Pièce comptable", pieceComptableId);
		}

		LignePieceComptableEntity entity = lignePieceComptableRepository.findById(ligneId)
				.orElseThrow(() -> new ResourceNotFoundException("Ligne pièce comptable", ligneId));

		if (!entity.getPieceComptable().getId().equals(pieceComptableId)) {
			throw new ResourceNotFoundException("Ligne pièce comptable " + ligneId + " n'appartient pas à la pièce comptable " + pieceComptableId);
		}

		ProduitEntity produit = null;
		if (request.getProduitId() != null) {
			produit = produitRepository.findById(request.getProduitId())
					.orElseThrow(() -> new ResourceNotFoundException("Produit", request.getProduitId()));
		}

		lignePieceComptableMapper.updateEntity(request, entity);
		entity.setProduit(produit);

		LignePieceComptableEntity saved = lignePieceComptableRepository.save(entity);
		return lignePieceComptableMapper.toDto(saved);
	}

	@Transactional
	public void deleteLigne(Long pieceComptableId, Long ligneId) {
		if (!pieceComptableRepository.existsById(pieceComptableId)) {
			throw new ResourceNotFoundException("Pièce comptable", pieceComptableId);
		}

		LignePieceComptableEntity entity = lignePieceComptableRepository.findById(ligneId)
				.orElseThrow(() -> new ResourceNotFoundException("Ligne pièce comptable", ligneId));

		if (!entity.getPieceComptable().getId().equals(pieceComptableId)) {
			throw new ResourceNotFoundException("Ligne pièce comptable " + ligneId + " n'appartient pas à la pièce comptable " + pieceComptableId);
		}

		lignePieceComptableRepository.deleteById(ligneId);
	}
}
