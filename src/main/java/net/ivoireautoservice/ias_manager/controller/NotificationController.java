package net.ivoireautoservice.ias_manager.controller;

import lombok.RequiredArgsConstructor;
import net.ivoireautoservice.ias_manager.dto.core.PagedResponse;
import net.ivoireautoservice.ias_manager.dto.core.notification.Notification;
import net.ivoireautoservice.ias_manager.services.notification.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Notifications de l'utilisateur connecté.
 *
 * <p>Pas de {@code @PreAuthorize} : ces endpoints sont auto-scopés sur
 * l'utilisateur authentifié (le service ne lit/modifie que <i>ses</i>
 * notifications) et ne protègent aucune ressource métier. Le RBAC intervient
 * en amont, au ciblage des destinataires.</p>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PagedResponse<Notification>> getMesNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        Pageable pageable = PageRequest.of(page, taille, Sort.by("createdAt").descending());
        return ResponseEntity.ok(notificationService.getMesNotifications(pageable));
    }

    @GetMapping("/non-lues/count")
    public ResponseEntity<Long> countNonLues() {
        return ResponseEntity.ok(notificationService.countMesNotificationsNonLues());
    }

    @PatchMapping("/{id}/lu")
    public ResponseEntity<Notification> marquerLu(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.marquerLu(id));
    }

    @PatchMapping("/lu")
    public ResponseEntity<Void> marquerToutLu() {
        notificationService.marquerToutLu();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        notificationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
