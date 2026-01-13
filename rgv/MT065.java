package fr.lbp.rgv.mt.format;

import org.apache.camel.dataformat.bindy.annotation.DataField;
import org.apache.camel.dataformat.bindy.annotation.FixedLengthRecord;

import lombok.Data;

@Data
@FixedLengthRecord(length = 625, paddingChar = ' ')
public class MT065 {

    @DataField(pos = 1, length = 16, trim = false, required = true)
    private String O_R1_Identification_emetteur;

    @DataField(pos = 17, length = 16, trim = false, required = true)
    private String O_R2_Reference_operation;

    @DataField(pos = 33, length = 16, trim = false, required = true)
    private String O_R3_Identification_recepteur;

    @DataField(pos = 49, length = 1, trim = false, required = false)
    private String F_R4_Type_emetteur;

    @DataField(pos = 50, length = 5, trim = false, required = true) // XSD: obligatoire
    private String O_R5_Code_operation;

    @DataField(pos = 55, length = 8, trim = false, required = true)
    private String O_Date_demande;

    @DataField(pos = 63, length = 6, trim = false, required = false)
    private String Z_Heure_demande;

    @DataField(pos = 69, length = 2, trim = false, required = false)
    private String F_Zone_reservee;

    @DataField(pos = 71, length = 12, trim = false, required = true)
    private String O_Code_etablissement_domiciliataire;

    @DataField(pos = 83, length = 5, trim = false, required = true)
    private String O_Code_adherent_domiciliataire;

    @DataField(pos = 88, length = 2, trim = false, required = true)
    private String O_Type_code_emetteur_Etablissement_emetteur;

    @DataField(pos = 90, length = 12, trim = false, required = true)
    private String O_Code_etablissement_Etablissement_emetteur;

    @DataField(pos = 102, length = 2, trim = false, required = false)
    private String F_Zone_reservee_Etablissement_emetteur;

    @DataField(pos = 104, length = 12, trim = false, required = false)
    private String F_Code_valeur_Etablissement_emetteur;

    @DataField(pos = 116, length = 3, trim = false, required = true)
    private String O_Type_valeur_TCN_Etablissement_emetteur;

    @DataField(pos = 119, length = 1, trim = false, required = true)
    private String O_Modalite_emission_TCN_Etablissement_emetteur;

    @DataField(pos = 120, length = 8, trim = false, required = true)
    private String O_Date_premiere_emission_Etablissement_emetteur;

    @DataField(pos = 128, length = 8, trim = false, required = true)
    private String O_Date_remboursement_TCN_Etablissement_emetteur;

    @DataField(pos = 136, length = 8, trim = false, required = false)
    private String Z_Date_premiere_jouissance_Etablissement_emetteur;

    @DataField(pos = 144, length = 8, trim = false, required = false)
    private String Z_Date_premier_paiement_interets_Etablissement_emetteur;

    @DataField(pos = 152, length = 1, trim = false, required = false)
    private String F_Type_remuneration_Etablissement_emetteur;

    @DataField(pos = 153, length = 3, trim = false, required = true)
    private String O_Code_taux_TCN_Etablissement_emetteur;

    @DataField(pos = 156, length = 3, trim = false, required = true)
    private String O_Code_devise_emission_Etablissement_emetteur;

    @DataField(pos = 159, length = 1, trim = false, required = true)
    private String O_Indicateur_admission_aux_systemes_ReglementLivraison_Etablissement_emetteur;

    @DataField(pos = 160, length = 2, trim = false, required = false)
    private String F_Format_Taux_interet_fixe;

    @DataField(pos = 162, length = 15, trim = false, required = false)
    private String Z_Montant_Taux_interet_fixe;

    @DataField(pos = 177, length = 80, trim = false, required = false)
    private String F_Commentaire_pour_TCN_Taux_interet_fixe;

    @DataField(pos = 257, length = 3, trim = false, required = false)
    private String F_Code_devise_Montant_unitaire_remboursement;

    @DataField(pos = 260, length = 2, trim = false, required = false)
    private String F_Format_Montant_unitaire_remboursement;

    @DataField(pos = 262, length = 15, trim = false, required = false)
    private String Z_Montant_Montant_unitaire_remboursement;

    @DataField(pos = 277, length = 1, trim = false, required = false)
    private String F_Code_sens_taux_Taux_marge_absolue;

    @DataField(pos = 278, length = 2, trim = false, required = false)
    private String F_Format_Taux_marge_absolue;

    @DataField(pos = 280, length = 15, trim = false, required = false)
    private String Z_Montant_Taux_marge_absolue;

    @DataField(pos = 295, length = 1, trim = false, required = false)
    private String F_Code_periodicite_interets_Taux_marge_absolue;

    @DataField(pos = 296, length = 1, trim = false, required = false)
    private String F_Type_interets_Taux_marge_absolue;

    @DataField(pos = 297, length = 1, trim = false, required = false)
    private String Guarantee;

    @DataField(pos = 298, length = 3, trim = false, required = false)
    private String Program_Originator;

    @DataField(pos = 301, length = 6, trim = false, required = false)
    private String Program_Identifier;

    @DataField(pos = 307, length = 16, trim = false, required = true)
    private String O_Reference_operation_placement_Caracteristiques_placement;

    @DataField(pos = 323, length = 5, trim = false, required = true)
    private String O_Code_adherent_partie_Caracteristiques_placement;

    @DataField(pos = 328, length = 2, trim = false, required = true)
    private String O_Type_souscompte_partie_Caracteristiques_placement;

    @DataField(pos = 330, length = 23, trim = false, required = true)
    private String O_Numero_souscompte_partie_Caracteristiques_placement;

    // ✅ FIX : SharedResource => minOccurs=0
    @DataField(pos = 353, length = 16, trim = false, required = false)
    private String F_Reference_interne_instruction_partie_Caracteristiques_placement;

    @DataField(pos = 369, length = 2, trim = false, required = false)
    private String F_Zone_reservee_Caracteristiques_placement;

    @DataField(pos = 371, length = 12, trim = false, required = true)
    private String O_Code_etablissement_contrepartie_Caracteristiques_placement;

    @DataField(pos = 383, length = 5, trim = false, required = false)
    private String F_Code_adherent_contrepartie_Caracteristiques_placement;

    @DataField(pos = 388, length = 2, trim = false, required = true)
    private String O_Code_type_instruction_Caracteristiques_placement;

    @DataField(pos = 390, length = 8, trim = false, required = true)
    private String O_Date_negociation_Caracteristiques_placement;

    @DataField(pos = 398, length = 6, trim = false, required = false)
    private String Z_Heure_negociation_Caracteristiques_placement;

    @DataField(pos = 404, length = 8, trim = false, required = true)
    private String O_Date_denouement_theorique_Caracteristiques_placement;

    @DataField(pos = 412, length = 6, trim = false, required = false)
    private String Z_Heure_denouement_theorique_Caracteristiques_placement;

    @DataField(pos = 418, length = 3, trim = false, required = true)
    private String O_Code_devise_Montant_emission_TCN;

    @DataField(pos = 421, length = 15, trim = false, required = true)
    private String O_Montant_Montant_emission_TCN;

    @DataField(pos = 436, length = 3, trim = false, required = true)
    private String O_Code_devise_Montant_net_ligne_instruction;

    @DataField(pos = 439, length = 15, trim = false, required = true)
    private String O_Montant_Montant_net_ligne_instruction;

    @DataField(pos = 454, length = 16, trim = false, required = false)
    private String F_Reference_clientele_Montant_net_ligne_instruction;

    @DataField(pos = 470, length = 50, trim = false, required = false)
    private String F_Commentaire_destine_contrepartie_Montant_net_ligne_instruction;

    @DataField(pos = 520, length = 2, trim = false, required = false)
    private String F_Type_code_externe_client_partie_Montant_net_ligne_instruction;

    @DataField(pos = 522, length = 23, trim = false, required = false)
    private String F_Code_externe_client_partie_Montant_net_ligne_instruction;

    @DataField(pos = 545, length = 28, trim = false, required = false)
    private String F_Nom_client_partie_Montant_net_ligne_instruction;

    @DataField(pos = 573, length = 2, trim = false, required = false)
    private String F_Type_code_externe_client_contrepartie_Montant_net_ligne_instruction;

    @DataField(pos = 575, length = 23, trim = false, required = false)
    private String F_Code_externe_client_contrepartie_Montant_net_ligne_instruction;

    @DataField(pos = 598, length = 28, trim = false, required = false)
    private String F_Nom_client_contrepartie_Montant_net_ligne_instruction;
}
