package net.ivoireautoservice.ias_manager.entity.notification;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.ivoireautoservice.ias_manager.entity.AuditableEntity;
import net.ivoireautoservice.ias_manager.entity.Utilisateur;
import net.ivoireautoservice.ias_manager.enums.notification.TypeNotificationEnum;

/**
 * Notification in-app d'un utilisateur.
 *
 * <p>Modèle « fan-out à l'écriture » : une notification diffusée à plusieurs
 * utilisateurs (ex. tous les détenteurs d'une permission) est dupliquée en une
 * ligne par destinataire. Le statut lu/non-lu et la suppression sont ainsi
 * naturellement individuels.</p>
 */
@Entity
@Table(name = "NOTIFICATIONS", uniqueConstraints = @UniqueConstraint(
		name = "uk_notification_destinataire_cle",
		columnNames = {"destinataire_id", "cle_dedoublonnage"}))
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"destinataire"})
@EqualsAndHashCode(callSuper = true, exclude = {"destinataire"})
public class NotificationEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destinataire_id", nullable = false)
	private Utilisateur destinataire;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TypeNotificationEnum type;

	@Column(nullable = false)
	private String titre;

	@Column(nullable = false, length = 500)
	private String message;

	/** Route du frontend vers la ressource concernée (ex. {@code /missions/12}). */
	private String lien;

	@Column(nullable = false)
	@Builder.Default
	private boolean lu = false;

	/**
	 * Empêche de recréer la même notification pour un même destinataire — le job
	 * d'expiration repasse chaque jour sur les mêmes échéances. Nullable : les
	 * notifications événementielles (mission créée) n'en ont pas besoin.
	 */
	@Column(name = "cle_dedoublonnage")
	private String cleDedoublonnage;
}
