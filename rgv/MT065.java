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
    p
