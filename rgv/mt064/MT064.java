package fr.lbp.rgv.mt.format;

import org.apache.camel.dataformat.bindy.annotation.DataField;
import org.apache.camel.dataformat.bindy.annotation.FixedLengthRecord;

import lombok.Data;

@Data
@FixedLengthRecord(length = 625, paddingChar = ' ')
public class MT064 {

    @DataField(pos = 1, length = 16, trim = false, required = true, align = "L")
    private String O_R1_Identification_emetteur;

    @DataField(pos = 17, length = 16, trim = false, required = true, align = "L")
    private String O_R2_Reference_operation;

    @DataField(pos = 33, length = 16, trim = false, required = true, align = "L")
    private String O_R3_Identification_recepteur;

    @DataField(pos = 49, length = 1, trim = false, required = false, align = "L")
    private String F_R4_Type_emetteur;

    @DataField(pos = 50, length = 5, trim = false, required = true, align = "L")
    private String O_R5_Code_operation;

    @DataField(pos = 55, length = 8, trim = false, required = true, align = "L")
    private String O_Date_demande_Codification;

    @DataField(pos = 63, length = 6, trim = false, required = false, align = "L")
    private String Z_Heure_demande_Codification;

    @DataField(pos = 69, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Codification;

    @DataField(pos = 71, length = 12, trim = false, required = true, align = "L")
    private String O_Code_etablissement_domiciliataire_Codification;

    @DataField(pos = 83, length = 5, trim = false, required = true, align = "L")
    private String O_Code_adherent_domiciliataire_Codification;

    @DataField(pos = 88, length = 2, trim = false, required = true, align = "L")
    private String O_Type_code_emetteur_Etablissement_emetteur;

    @DataField(pos = 90, length = 12, trim = false, required = true, align = "L")
    private String O_Code_etablissement_Etablissement_emetteur;

    @DataField(pos = 102, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Etablissement_emetteur;

    @DataField(pos = 104, length = 12, trim = false, required = false, align = "L")
    private String F_Code_valeur_Etablissement_emetteur;

    @DataField(pos = 116, length = 3, trim = false, required = true, align = "L")
    private String O_Type_valeur_TCN_Etablissement_emetteur;

    @DataField(pos = 119, length = 1, trim = false, required = true, align = "L")
    private String O_Modalite_emission_TCN_Etablissement_emetteur;

    @DataField(pos = 120, length = 8, trim = false, required = true, align = "L")
    private String O_Date_premiere_emission_Etablissement_emetteur;

    @DataField(pos = 128, length = 8, trim = false, required = true, align = "L")
    private String O_Date_remboursement_TCN_Etablissement_emetteur;

    @DataField(pos = 136, length = 8, trim = false, required = false, align = "L")
    private String Z_Date_premiere_jouissance_Etablissement_emetteur;

    @DataField(pos = 144, length = 8, trim = false, required = false, align = "L")
    private String Z_Date_premier_paiement_interets_Etablissement_emetteur;

    @DataField(pos = 152, length = 1, trim = false, required = false, align = "L")
    private String F_Type_remuneration_Etablissement_emetteur;

    @DataField(pos = 153, length = 3, trim = false, required = true, align = "L")
    private String O_Code_taux_TCN_Etablissement_emetteur;

    @DataField(pos = 156, length = 3, trim = false, required = true, align = "L")
    private String O_Code_devise_emission_Etablissement_emetteur;

    @DataField(pos = 159, length = 1, trim = false, required = true, align = "L")
    private String O_Indicateur_admission_aux_systemes_Reglement_Livraison_Etablissement_emetteur;

    @DataField(pos = 160, length = 2, trim = false, required = false, align = "L")
    private String F_Format_Taux_interet_fixe;

    @DataField(pos = 162, length = 15, trim = false, required = false, align = "L")
    private String Z_Montant_Taux_interet_fixe;

    @DataField(pos = 177, length = 80, trim = false, required = false, align = "L")
    private String F_Commentaire_pour_TCN_Taux_interet_fixe;

    @DataField(pos = 257, length = 3, trim = false, required = false, align = "L")
    private String F_Code_devise_Montant_unitaire_remboursement;

    @DataField(pos = 260, length = 2, trim = false, required = false, align = "L")
    private String F_Format_Montant_unitaire_remboursement;

    @DataField(pos = 262, length = 15, trim = false, required = false, align = "L")
    private String Z_Montant_Montant_unitaire_remboursement;

    @DataField(pos = 277, length = 1, trim = false, required = false, align = "L")
    private String F_Code_sens_taux_Taux_marge_absolue;

    @DataField(pos = 278, length = 2, trim = false, required = false, align = "L")
    private String F_Format_Taux_marge_absolue;

    @DataField(pos = 280, length = 15, trim = false, required = false, align = "L")
    private String Z_Montant_Taux_marge_absolue;

    @DataField(pos = 295, length = 1, trim = false, required = false, align = "L")
    private String F_Code_periodicite_interets_Taux_marge_absolue;

    @DataField(pos = 296, length = 1, trim = false, required = false, align = "L")
    private String F_Type_interets_Taux_marge_absolue;

    @DataField(pos = 297, length = 1, trim = false, required = false, align = "L")
    private String Guarantee;

    @DataField(pos = 298, length = 3, trim = false, required = false, align = "L")
    private String Program_Originator;

    @DataField(pos = 301, length = 6, trim = false, required = false, align = "L")
    private String Program_Identifier;

    @DataField(pos = 307, length = 16, trim = false, required = true, align = "L")
    private String O_Reference_operation_emission_Caracteristiques_emission;

    @DataField(pos = 323, length = 3, trim = false, required = true, align = "L")
    private String O_Code_devise_Montant_emission_TCN;

    @DataField(pos = 326, length = 15, trim = false, required = true, align = "L")
    private String O_Montant_Montant_emission_TCN;

    @DataField(pos = 341, length = 5, trim = false, required = true, align = "L")
    private String O_Code_adherent_etablissement_debite_Montant_emission_TCN;

    @DataField(pos = 346, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Compte_titres_credite;

    @DataField(pos = 348, length = 12, trim = false, required = true, align = "L")
    private String O_Code_etablissement_Compte_titres_credite;

    @DataField(pos = 360, length = 2, trim = false, required = true, align = "L")
    private String O_Type_sous_compte_Compte_titres_credite;

    @DataField(pos = 362, length = 23, trim = false, required = true, align = "L")
    private String O_Numero_sous_compte_Compte_titres_credite;

    @DataField(pos = 385, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Compte_titres_credite_1;

    @DataField(pos = 387, length = 12, trim = false, required = false, align = "L")
    private String F_Code_valeur_Compte_titres_credite;

    @DataField(pos = 399, length = 3, trim = false, required = false, align = "L")
    private String Z_Code_nature_compte_Compte_titres_credite;

    @DataField(pos = 402, length = 1, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Compte_titres_credite_2;

    @DataField(pos = 403, length = 1, trim = false, required = false, align = "L")
    private String F_ICPG_Compte_titres_credite;

    @DataField(pos = 404, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Compte_titres_debite;

    @DataField(pos = 406, length = 12, trim = false, required = true, align = "L")
    private String O_Code_etablissement_Compte_titres_debite;

    @DataField(pos = 418, length = 2, trim = false, required = true, align = "L")
    private String O_Type_sous_compte_Compte_titres_debite;

    @DataField(pos = 420, length = 23, trim = false, required = true, align = "L")
    private String O_Numero_sous_compte_Compte_titres_debite;

    @DataField(pos = 443, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Compte_titres_debite_1;

    @DataField(pos = 445, length = 12, trim = false, required = false, align = "L")
    private String F_Code_valeur_Compte_titres_debite;

    @DataField(pos = 457, length = 3, trim = false, required = false, align = "L")
    private String Z_Code_nature_compte_Compte_titres_debite;

    @DataField(pos = 460, length = 1, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Compte_titres_debite_2;

    @DataField(pos = 461, length = 1, trim = false, required = false, align = "L")
    private String F_ICPG_Compte_titres_debite;

    @DataField(pos = 462, length = 8, trim = false, required = true, align = "L")
    private String O_Date_denouement_theorique_Compte_titres_debite;

    @DataField(pos = 470, length = 6, trim = false, required = false, align = "L")
    private String Z_Heure_denouement_theorique_Compte_titres_debite;

    @DataField(pos = 476, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Reference_livre;

    @DataField(pos = 478, length = 12, trim = false, required = false, align = "L")
    private String F_Code_etablissement_Reference_livre;

    @DataField(pos = 490, length = 5, trim = false, required = false, align = "L")
    private String F_Code_adherent_Reference_livre;

    @DataField(pos = 495, length = 16, trim = false, required = false, align = "L")
    private String F_Reference_operation_chez_etablissement_Reference_livre;

    @DataField(pos = 511, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee_Reference_livreur;

    @DataField(pos = 513, length = 12, trim = false, required = true, align = "L")
    private String O_Code_etablissement_Reference_livreur;

    @DataField(pos = 525, length = 5, trim = false, required = true, align = "L")
    private String O_Code_adherent_Reference_livreur;

    @DataField(pos = 530, length = 16, trim = false, required = true, align = "L")
    private String O_Reference_operation_chez_etablissement_Reference_livreur;

    @DataField(pos = 546, length = 80, trim = false, required = false, align = "L")
    private String F_Commentaire_Reference_livreur;
}
