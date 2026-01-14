package fr.lbp.rgv.mt.format;

import org.apache.camel.dataformat.bindy.annotation.DataField;
import org.apache.camel.dataformat.bindy.annotation.FixedLengthRecord;

import lombok.Data;

@Data
@FixedLengthRecord(length = 143, paddingChar = ' ')
public class MT066 {

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

    @DataField(pos = 55, length = 26, trim = false, required = true, align = "L")
    private String O_Horodatage;

    @DataField(pos = 81, length = 16, trim = false, required = true, align = "L")
    private String O_Reference_operation_demande_codification;

    @DataField(pos = 97, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee;

    @DataField(pos = 99, length = 12, trim = false, required = true, align = "L")
    private String O_Code_etablissement_domiciliataire;

    @DataField(pos = 111, length = 5, trim = false, required = true, align = "L")
    private String O_Code_adherent_domiciliataire;

    @DataField(pos = 116, length = 2, trim = false, required = false, align = "L")
    private String F_Zone_reservee_1;

    @DataField(pos = 118, length = 12, trim = false, required = true, align = "L")
    private String O_Code_valeur;

    @DataField(pos = 130, length = 8, trim = false, required = true, align = "L")
    private String O_Date_effet_referentiel;

    @DataField(pos = 138, length = 6, trim = false, required = true, align = "L")
    private String O_Heure_effet_referentiel;
}
