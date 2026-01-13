```java
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperties;
import org.apache.commons.lang3.StringUtils;

// ... autres imports selon ton projet

public void generateMTMessage(RGVmessage rgvMessage,
                              @ExchangeProperties Map<String, Object> properties,
                              Exchange exchange) {

    MT065 result = new MT065();

    // Normalisation "BW-like" du codeTaux
    String codeTaux = trimSafe(rgvMessage.getRGVchamps().getCodeTauxTCN());

    // ===== RGVcommun =====
    result.setO_R1_Identification_emetteur(rgvMessage.getRGVcommun().getIdentificationEmetteur());
    result.setO_R2_Reference_operation(rgvMessage.getRGVcommun().getReferenceOperation());
    result.setO_R3_Identification_recepteur(rgvMessage.getRGVcommun().getIdentificationRecepteur());
    result.setF_R4_Type_emetteur(rgvMessage.getRGVcommun().getTypeEmetteur());
    result.setO_R5_Code_operation(rgvMessage.getRGVcommun().getCodeOperation());

    // ===== Codification =====
    result.setO_Date_demande(rgvMessage.getRGVchamps().getCodification().getDateDemande());
    result.setZ_Heure_demande(rgvMessage.getRGVchamps().getCodification().getHeureDemande());

    // BW: <xsl:if test=".../zoneReserve">
    String zoneReserve = rgvMessage.getRGVchamps().getCodification().getZoneReserve();
    if (isPresent(zoneReserve)) {
        result.setF_Zone_reservee(zoneReserve);
    }

    result.setO_Code_etablissement_domiciliataire(
        rgvMessage.getRGVchamps().getCodification().getCodeEtablissementDomiciliataire()
    );
    result.setO_Code_adherent_domiciliataire(
        rgvMessage.getRGVchamps().getCodification().getCodeAdherentDomiciliataire()
    );

    // ===== Etablissement emetteur =====
    result.setO_Type_code_emetteur_Etablissement_emetteur(
        rgvMessage.getRGVchamps().getEtablissementEmetteur().getTypeCode()
    );
    result.setO_Code_etablissement_Etablissement_emetteur(
        rgvMessage.getRGVchamps().getEtablissementEmetteur().getCodeEtablissement()
    );

    // BW: <xsl:if test="$Start/.../RGVchamps/zoneReservee">
    String zoneReserveeEtab = rgvMessage.getRGVchamps().getZoneReservee();
    if (isPresent(zoneReserveeEtab)) {
        result.setF_Zone_reservee_Etablissement_emetteur(zoneReserveeEtab);
    }

    // BW: <xsl:if test="$Start/.../RGVchamps/codeValeur">
    String codeValeur = rgvMessage.getRGVchamps().getCodeValeur();
    if (isPresent(codeValeur)) {
        result.setF_Code_valeur_Etablissement_emetteur(codeValeur);
    }

    result.setO_Type_valeur_TCN_Etablissement_emetteur(rgvMessage.getRGVchamps().getTypeValeurTCN());
    result.setO_Modalite_emission_TCN_Etablissement_emetteur(rgvMessage.getRGVchamps().getModaliteEmissionTCN());

    // BW: pad-front(..., '8', '0')
    result.setO_Date_premiere_emission_Etablissement_emetteur(
        padLeftZero(rgvMessage.getRGVchamps().getDatePremiereEmission(), 8)
    );
    result.setO_Date_remboursement_TCN_Etablissement_emetteur(
        padLeftZero(rgvMessage.getRGVchamps().getDateRemboursementTCN(), 8)
    );

    // BW:
    // if(contains("003-015-016", trim(codeTaux))) then "00000000" else pad-front(datePremiereJouissance, 8, '0')
    result.setZ_Date_premiere_jouissance_Etablissement_emetteur(
        CODE_TAUX_003_015_016.contains(codeTaux)
            ? "00000000"
            : padLeftZero(rgvMessage.getRGVchamps().getDatePremiereJouissance(), 8)
    );

    // BW:
    // if(contains("000-035", trim(codeTaux))) then pad-front(datePremierPaiementInterets,8,'0') else "00000000"
    result.setZ_Date_premier_paiement_interets_Etablissement_emetteur(
        CODE_TAUX_000_035.contains(codeTaux)
            ? padLeftZero(rgvMessage.getRGVchamps().getDatePremierPaiementInterets(), 8)
            : "00000000"
    );

    // BW: choose
    // when typeValeurTCN='NCP' => typeRemuneration
    // else => if contains("000-035", codeTaux) then typeRemuneration else " "
    if ("NCP".equals(rgvMessage.getRGVchamps().getTypeValeurTCN())) {
        result.setF_Type_remuneration_Etablissement_emetteur(
            rgvMessage.getRGVchamps().getTypeRemuneration()
        );
    } else {
        result.setF_Type_remuneration_Etablissement_emetteur(
            CODE_TAUX_000_035.contains(codeTaux)
                ? rgvMessage.getRGVchamps().getTypeRemuneration()
                : " "
        );
    }

    result.setO_Code_taux_TCN_Etablissement_emetteur(rgvMessage.getRGVchamps().getCodeTauxTCN());
    result.setO_Code_devise_emission_Etablissement_emetteur(rgvMessage.getRGVchamps().getCodeDeviseEmission());
    result.setO_Indicateur_admission_aux_systemes_ReglementLivraison_Etablissement_emetteur(
        rgvMessage.getRGVchamps().getIndicateurAdmissionSystemesRL()
    );

    // ===== Taux interet fixe =====
    double tauxFixeMontant = toDoubleSafe(rgvMessage.getRGVchamps().getTauxInteretFixe().getMontant());
    result.setF_Format_Taux_interet_fixe(
        tauxFixeMontant == 0d ? "" : rgvMessage.getRGVchamps().getTauxInteretFixe().getFormat()
    );
    result.setZ_Montant_Taux_interet_fixe(
        padLeftZero(rgvMessage.getRGVchamps().getTauxInteretFixe().getMontant(), 15)
    );

    // BW: <xsl:if test=".../commentaireTCN">
    String commentaireTCN = rgvMessage.getRGVchamps().getTauxInteretFixe().getCommentaireTCN();
    if (isPresent(commentaireTCN)) {
        result.setF_Commentaire_pour_TCN_Taux_interet_fixe(commentaireTCN);
    }

    // ===== Montant unitaire remboursement =====
    double murMontant = toDoubleSafe(rgvMessage.getRGVchamps().getMontantUnitaireRemboursement().getMontant());
    boolean murApplicable = CODE_TAUX_000_035_015_016.contains(codeTaux) && murMontant != 0d;

    // BW:
    // if(not(contains("000-035-015-016", trim(codeTaux))) or number(montant)=0) then "" else codeDevise/format
    result.setF_Code_devise_Montant_unitaire_remboursement(
        murApplicable ? rgvMessage.getRGVchamps().getMontantUnitaireRemboursement().getCodeDevise() : ""
    );
    result.setF_Format_Montant_unitaire_remboursement(
        murApplicable ? rgvMessage.getRGVchamps().getMontantUnitaireRemboursement().getFormat() : ""
    );

    // BW:
    // if(contains("000-035-015-016", trim(codeTaux))) then pad-front(montant,15,'0') else "000000000000000"
    result.setZ_Montant_Montant_unitaire_remboursement(
        CODE_TAUX_000_035_015_016.contains(codeTaux)
            ? padLeftZero(rgvMessage.getRGVchamps().getMontantUnitaireRemboursement().getMontant(), 15)
            : "000000000000000"
    );

    // ===== Taux marge absolue =====
    double tmaMontant = toDoubleSafe(rgvMessage.getRGVchamps().getTauxMargeAbsolue().getMontant());

    // BW: if(number(montant)=0) then "" else codeSens/format
    result.setF_Code_sens_taux_Taux_marge_absolue(
        tmaMontant == 0d ? "" : rgvMessage.getRGVchamps().getTauxMargeAbsolue().getCodeSens()
    );
    result.setF_Format_Taux_marge_absolue(
        tmaMontant == 0d ? "" : rgvMessage.getRGVchamps().getTauxMargeAbsolue().getFormat()
    );
    result.setZ_Montant_Taux_marge_absolue(
        padLeftZero(rgvMessage.getRGVchamps().getTauxMargeAbsolue().getMontant(), 15)
    );

    // BW: if contains("000-035", trim(codeTaux)) then value else " "
    result.setF_Code_periodicite_interets_Taux_marge_absolue(
        CODE_TAUX_000_035.contains(codeTaux) ? rgvMessage.getRGVchamps().getCodePeriodiciteInterets() : " "
    );
    result.setF_Type_interets_Taux_marge_absolue(
        CODE_TAUX_000_035.contains(codeTaux) ? rgvMessage.getRGVchamps().getTypeInterets() : " "
    );

    // ===== Divers =====
    result.setGuarantee(rgvMessage.getRGVchamps().getEtablissementEmetteur().getGarantee());
    result.setProgram_Originator(
        padLeftSpace(rgvMessage.getRGVchamps().getEtablissementEmetteur().getProgramOrigin(), 3)
    );
    result.setProgram_Identifier(
        padLeftSpace(rgvMessage.getRGVchamps().getEtablissementEmetteur().getProgramId(), 6)
    );

    // ===== Caracteristiques placement =====
    result.setO_Reference_operation_placement_Caracteristiques_placement(
        rgvMessage.getRGVchamps().getCaracteristiquesEP().getReferenceOperation()
    );
    result.setO_Code_adherent_partie_Caracteristiques_placement(
        rgvMessage.getRGVchamps().getCaracteristiquesEP().getCodeAdherentPartie()
    );
    result.setO_Type_souscompte_partie_Caracteristiques_placement(
        rgvMessage.getRGVchamps().getCaracteristiquesEP().getTypeSousComptePartie()
    );

    // BW: if(number(numeroSousComptePartie)=0) then "0" else value
    double numeroSousCompte = toDoubleSafe(rgvMessage.getRGVchamps().getCaracteristiquesEP().getNumeroSousComptePartie());
    result.setO_Numero_souscompte_partie_Caracteristiques_placement(
        numeroSousCompte == 0d ? "0" : rgvMessage.getRGVchamps().getCaracteristiquesEP().getNumeroSousComptePartie()
    );

    // BW: xsl:if referenceInterneInstructionPartie
    String refInterneInstr = rgvMessage.getRGVchamps().getCaracteristiquesEP().getReferenceInterneInstructionPartie();
    if (isPresent(refInterneInstr)) {
        result.setF_Reference_interne_instruction_partie_Caracteristiques_placement(refInterneInstr);
    }

    // BW: xsl:if zoneReservee caracteristiquesEP
    String zoneReserveeCarac = rgvMessage.getRGVchamps().getCaracteristiquesEP().getZoneReservee();
    if (isPresent(zoneReserveeCarac)) {
        result.setF_Zone_reservee_Caracteristiques_placement(zoneReserveeCarac);
    }

    result.setO_Code_etablissement_contrepartie_Caracteristiques_placement(
        rgvMessage.getRGVchamps().getCodeEtablissementContrepartie()
    );

    // BW: if(number(codeAdherentContrepartie)=0) then "" else value
    double codeAdhCp = toDoubleSafe(rgvMessage.getRGVchamps().getCodeAdherentContrepartie());
    result.setF_Code_adherent_contrepartie_Caracteristiques_placement(
        codeAdhCp == 0d ? "" : rgvMessage.getRGVchamps().getCodeAdherentContrepartie()
    );

    result.setO_Code_type_instruction_Caracteristiques_placement(
        rgvMessage.getRGVchamps().getCodeTypeInstruction()
    );
    result.setO_Date_negociation_Caracteristiques_placement(
        padLeftZero(rgvMessage.getRGVchamps().getDateNegociation(), 8)
    );
    result.setZ_Heure_negociation_Caracteristiques_placement(
        padLeftZero(rgvMessage.getRGVchamps().getHeureNegociation(), 6)
    );
    result.setO_Date_denouement_theorique_Caracteristiques_placement(
        padLeftZero(rgvMessage.getRGVchamps().getDateDenouementTheorique(), 8)
    );
    result.setZ_Heure_denouement_theorique_Caracteristiques_placement(
        padLeftZero(rgvMessage.getRGVchamps().getHeureDenouementTheorique(), 6)
    );

    // ===== Montants =====
    result.setO_Code_devise_Montant_emission_TCN(
        rgvMessage.getRGVchamps().getMontantEmissionTCN().getCodeDevise()
    );
    result.setO_Montant_Montant_emission_TCN(
        padLeftZero(rgvMessage.getRGVchamps().getMontantEmissionTCN().getMontant(), 15)
    );

    result.setO_Code_devise_Montant_net_ligne_instruction(
        rgvMessage.getRGVchamps().getMontantNetLigneInstruction().getDevise()
    );
    result.setO_Montant_Montant_net_ligne_instruction(
        padLeftZero(rgvMessage.getRGVchamps().getMontantNetLigneInstruction().getMontant(), 15)
    );

    // BW: xsl:if referenceDeClientele
    String referenceDeClientele = rgvMessage.getRGVchamps().getReferenceDeClientele();
    if (isPresent(referenceDeClientele)) {
        result.setF_Reference_clientele_Montant_net_ligne_instruction(referenceDeClientele);
    }

    // BW: referenceClientele/* en xsl:if
    if (rgvMessage.getRGVchamps().getReferenceClientele() != null) {
        var rc = rgvMessage.getRGVchamps().getReferenceClientele();

        if (isPresent(rc.getCommentaireDestineContrepartie())) {
            result.setF_Commentaire_destine_contrepartie_Montant_net_ligne_instruction(rc.getCommentaireDestineContrepartie());
        }
        if (isPresent(rc.getTypeCodeExterneClientPartie())) {
            result.setF_Type_code_externe_client_partie_Montant_net_ligne_instruction(rc.getTypeCodeExterneClientPartie());
        }
        if (isPresent(rc.getCodeExterneClientPartie())) {
            result.setF_Code_externe_client_partie_Montant_net_ligne_instruction(rc.getCodeExterneClientPartie());
        }
        if (isPresent(rc.getNomClientPartie())) {
            result.setF_Nom_client_partie_Montant_net_ligne_instruction(rc.getNomClientPartie());
        }
        if (isPresent(rc.getTypeCodeExterneClientContrepartie())) {
            result.setF_Type_code_externe_client_contrepartie_Montant_net_ligne_instruction(rc.getTypeCodeExterneClientContrepartie());
        }
        if (isPresent(rc.getCodeExterneClientContrepartie())) {
            result.setF_Code_externe_client_contrepartie_Montant_net_ligne_instruction(rc.getCodeExterneClientContrepartie());
        }
        if (isPresent(rc.getNomClientContrepartie())) {
            result.setF_Nom_client_contrepartie_Montant_net_ligne_instruction(rc.getNomClientContrepartie());
        }
    }

    // Mets le résultat dans le body (adapte si ton flux utilise properties/header)
    exchange.getIn().setBody(result);
}

/* ============================
   Utilitaires (helpers sortis)
   ============================ */

private static final Set<String> CODE_TAUX_000_035 =
        Set.of("000", "035");

private static final Set<String> CODE_TAUX_003_015_016 =
        Set.of("003", "015", "016");

private static final Set<String> CODE_TAUX_000_035_015_016 =
        Set.of("000", "035", "015", "016");

private static String trimSafe(String s) {
    return s == null ? "" : s.trim();
}

private static boolean isPresent(String s) {
    return s != null && !s.isEmpty();
}

/**
 * BW5: number("") => 0
 * Ici: null/blank/invalid => 0, sinon Double.parse
 */
private static double toDoubleSafe(String s) {
    if (s == null) return 0d;
    String t = s.trim();
    if (t.isEmpty()) return 0d;
    try {
        return Double.valueOf(t);
    } catch (Exception e) {
        return 0d;
    }
}

private static String padLeftZero(String s, int length) {
    return StringUtils.leftPad(s == null ? "" : s, length, "0");
}

private static String padLeftSpace(String s, int length) {
    return StringUtils.leftPad(s == null ? "" : s, length, " ");
}
```
