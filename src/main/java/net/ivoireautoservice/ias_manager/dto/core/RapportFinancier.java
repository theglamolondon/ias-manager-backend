package net.ivoireautoservice.ias_manager.dto.core;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RapportFinancier {

	// KPIs principaux
	private long chiffreAffaire;
	private long chiffreAffaireHt;
	private long montantEncaisse;
	private long nombreFacturesEncaissees;
	private long montantAVenir;
	private long nombreFacturesAVenir;
	private long montantEnRetard;
	private long nombreFacturesEnRetard;
	private double pourcentageRecouvrement;
	private long nombreTotalFactures;
	private double dsoJours;

	// KPIs dérivés (bloc 4)
	private long ticketMoyen;
	private double tauxImpayes;

	// Bloc 2 : marge / résultat
	private long totalDepenses;
	private long resultatNet;
	private double tauxMarge;

	// Balance âgée (clients)
	private BalanceAgee balanceAgee;

	// Bloc 1 : volet fournisseur / dettes
	private DettesFournisseur dettesFournisseur;
	private BalanceAgee balanceAgeeFournisseur;

	// Bloc 2 : trésorerie & flux
	private Tresorerie tresorerie;
	private FluxPeriode fluxPeriode;

	// Bloc 3 : avoirs & annulations
	private AvoirsAnnulations avoirsAnnulations;

	// Bloc 4 : TVA
	private Tva tva;

	// Rentrées de fonds réelles vs estimées (3 mois)
	private List<RentreeFonds> rentreesFonds;

	// Évolution DSO mensuel
	private List<DsoMensuel> evolutionDso;

	// 5 plus anciennes factures échues
	private List<FactureEchue> facturesEchues;

	// Top 5 clients par solde impayé
	private List<ClientImpaye> topClientsImpayes;

	// Montant à relancer
	private MontantRelance montantRelance;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class BalanceAgee {
		private long nonEchu;
		private long echu0a30;
		private long echu31a60;
		private long echu61a90;
		private long echuPlus90;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class DettesFournisseur {
		private long totalFacture;
		private long totalPaye;
		private long totalDu;
		private long totalDuEchu;
		private long totalDuAVenir;
		private long nombre;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class Tresorerie {
		private long soldeTotal;
		private long soldesPositifs;
		private long soldesNegatifs;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class FluxPeriode {
		private long encaissements;
		private long decaissements;
		private long remboursements;
		private long fluxNet;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class AvoirsAnnulations {
		private long avoirsMontant;
		private long avoirsNombre;
		private long caPerduAnnulations;
		private long missionsAnnuleesNombre;
		private double tauxAnnulation;
		private long remboursementsMontant;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class Tva {
		private long collectee;
		private long deductible;
		private long aReverser;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class RentreeFonds {
		private int mois;
		private int annee;
		private long chiffreAffaire;
		private long encaissement;
		private long prevision;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class DsoMensuel {
		private int mois;
		private int annee;
		private double dso;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class FactureEchue {
		private Long factureId;
		private String numFacture;
		private String nomClient;
		private long joursRetard;
		private long solde;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class ClientImpaye {
		private Long partenaireId;
		private String nomClient;
		private long montantImpaye;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	public static class MontantRelance {
		private long totalMontant;
		private long totalNombre;
		private long relance1Montant;
		private long relance1Nombre;
		private long relance2Montant;
		private long relance2Nombre;
		private long relance3Montant;
		private long relance3Nombre;
	}
}