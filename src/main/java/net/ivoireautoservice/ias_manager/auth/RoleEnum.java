package net.ivoireautoservice.ias_manager.auth;

/**
 * Liste canonique des <b>noms de rôles système</b> amorcés au démarrage par
 * {@code RbacSeeder}. Ce n'est plus la source des autorités de sécurité (celles-ci
 * proviennent désormais des {@code RoleEntity}/{@link PermissionEnum} en base) ; cet
 * enum sert uniquement de référence stable pour le seeding du socle de rôles.
 */
public enum RoleEnum {
    ADMIN, LOGISTIQUE, COMMERCIAL, RECOUVREMENT, RH, ACHAT
}
