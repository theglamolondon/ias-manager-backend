package net.ivoireautoservice.ias_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "EMPLOYES")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"service"})
@EqualsAndHashCode(callSuper = true, exclude = {"service"})
public class EmployeEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String matricule;

	@Column(nullable = false)
	private String nom;

	@Column(nullable = false)
	private String prenoms;

	private String photo;
	private LocalDate dateNaissance;
	private String rib;
	private String numeroCnps;
	private LocalDate dateEmbauche;
	private LocalDate dateDepart;
	private String telephone1;
	private String telephone2;
	private String email;
	private String lieuNaissance;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "service_id")
	private ServiceEntity service;
}
