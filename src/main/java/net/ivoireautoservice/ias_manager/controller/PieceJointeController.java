package net.ivoireautoservice.ias_manager.controller;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.PieceJointe;
import net.ivoireautoservice.ias_manager.enums.PieceJointeOwnerTypeEnum;
import net.ivoireautoservice.ias_manager.services.PieceJointeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pieces-jointes")
@RequiredArgsConstructor
public class PieceJointeController {

    private final PieceJointeService pieceJointeService;

    @GetMapping
    public ResponseEntity<List<PieceJointe>> getByOwner(
            @RequestParam PieceJointeOwnerTypeEnum ownerType,
            @RequestParam Long ownerId) {
        return ResponseEntity.ok(pieceJointeService.getByOwner(ownerType, ownerId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PieceJointe> upload(
            @RequestParam PieceJointeOwnerTypeEnum ownerType,
            @RequestParam Long ownerId,
            @RequestParam("file") MultipartFile file) {
        PieceJointe created = pieceJointeService.upload(ownerType, ownerId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pieceJointeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
