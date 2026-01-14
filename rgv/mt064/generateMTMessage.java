import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperties;
import org.apache.commons.lang3.StringUtils;

public void generateMTMessage(RGVmessage rgvMessage,
                              @ExchangeProperties Map<String, Object> properties,
                              Exchange exchange) {

    MT064 result = new MT064();

    // --- Helpers "BW-like"
    String codeTaux = trimSafe(rgvMessage.getRGVchamps().getCodeTauxTCN()); // tib:trim
    // NB: BW compare à '035' exactement et fait contains sur "000-035-015-016"
    // donc on ne pad pas ici si l’amont fournit déjà 3 digits.
    // Si tu as des cas "35", alors fais: codeTaux = StringUtils.leftPad(codeTaux, 3, '0');

    // ===== RGVcommun =====
    result.setO_R1_Identification_emetteur(rgvMessage.getRGVcommun().getIdentificationEmetteur());
    result.setO_R2_Reference_operation(rgvMessage.getRGVcommun().getReferenceOperation());
    result.setO_R3_Identification_recepteur(rgvMessage.getRGVcommun().getIdentificationRecepteur());
    result.setF_R4_Type_emetteur(rgvMessage.getRGVcommun().getTypeEmetteur());
    result.setO_R5_Code_operation(rgvMessage.getRGVcommun().getCodeOperation());

    // ===== Codification =====
    result.setO_Date_demande_Codification(rgvMessage.getRGVchamps().getCodification().getDateDemande());
    result.setZ_Heure_demande_Codification(rgvMessage.getRGVchamps().getCodification().getHeureDemande());

    String zoneReserveCodif = rgvMessage.getRGVchamps().getCodification().getZoneReserve();
    if (isPresent(zoneReserveCodif)) {
        result.setF_Zone_reservee_Codification(zoneReserveCodif);
    }

    result.setO_Code_etablissement_domiciliataire_Codification(
        rgvMessage.getRGVchamps().getCodification().getCodeEtablissementDomiciliataire()
    );
    result.setO_Code_adherent_domiciliataire_Codification(
        rgvMessage.getRGVchamps().getCodification().getCodeAdherentDomiciliataire()
    );

    // ===== Etablissement emetteur =====
    result.setO_Type_code_emetteur_Etablissement_emetteur(
        rgvMessage.getRGVchamps().getEtablissementEmetteur().getTypeCode()
    );
    result.setO_Code_etablissement_Etablissement_emetteur(
        rgvMessage.getRGVchamps().getEtablissementEmetteur().getCodeEtablissement()
    );

    String zoneReserveeEtab = rgvMessage.getRGVchamps().getZoneReservee();
    if (isPresent(zoneReserveeEtab)) {
        result.setF_Zone_reservee_Etablissement_emetteur(zoneReserveeEtab);
    }

    String codeValeurEtab = rgvMessage.getRGVchamps().getCodeValeur();
    if (isPresent(codeValeurEtab)) {
        result.setF_Code_valeur_Etablissement_emetteur(codeValeurEtab);
    }

    result.setO_Type_valeur_TCN_Etablissement_emetteur(rgvMessage.getRGVchamps().getTypeValeurTCN());
    result.setO_Modalite_emission_TCN_Etablissement_emetteur(rgvMessage.getRGVchamps().getModaliteEmissionTCN());

    // tib:pad-front(..., '8', '0')
    result.setO_Date_premiere_emission_Etablissement_emetteur(
        StringUtils.leftPad(nullToEmpty(rgvMessage.getRGVchamps().getDatePremiereEmission()), 8, '0')
    );
    result.setO_Date_remboursement_TCN_Etablissement_emetteur(
        StringUtils.leftPad(nullToEmpty(rgvMessage.getRGVchamps().getDateRemboursementTCN()), 8, '0')
    );

    // if contains("003-015-016", trim(codeTaux)) then "00000000" else pad-front(datePremiereJouissance,8,'0')
    result.setZ_Date_premiere_jouissance_Etablissement_emetteur(
        containsInDashList("003-015-016", codeTaux)
            ? "00000000"
            : StringUtils.leftPad(nullToEmpty(rgvMessage.getRGVchamps().getDatePremiereJouissance()), 8, '0')
    );

    // if contains("000-035", trim(codeTaux)) then pad-front(datePremierPaiementInterets,8,'0') else "00000000"
    result.setZ_Date_premier_paiement_interets_Etablissement_emetteur(
        containsInDashList("000-035", codeTaux)
            ? StringUtils.leftPad(nullToEmpty(rgvMessage.getRGVchamps().getDatePremierPaiementInterets()), 8, '0')
            : "00000000"
    );

    // ===== Type remuneration (spécificité MT064) =====
    // choose:
    // 1) when '035' = trim(codeTaux) and typeValeurTCN='NCP' => 'V'
    // 2) when typeValeurTCN='NCP' => typeRemuneration
    // 3) otherwise => if contains("000-035", trim(codeTaux)) then typeRemuneration else " "
    String typeValeur = rgvMessage.getRGVchamps().getTypeValeurTCN();
    if ("035".equals(codeTaux) && "NCP".equals(typeValeur)) {
        result.setF_Type_remuneration_Etablissement_emetteur("V");
    } else if ("NCP".equals(typeValeur)) {
        result.setF_Type_remuneration_Etablissement_emetteur(rgvMessage.getRGVchamps().getTypeRemuneration());
    } else {
        result.setF_Type_remuneration_Etablissement_emetteur(
            containsInDashList("000-035", codeTaux)
                ? rgvMessage.getRGVchamps().getTypeRemuneration()
                : " "
        );
    }

    result.setO_Code_taux_TCN_Etablissement_emetteur(rgvMessage.getRGVchamps().getCodeTauxTCN());
    result.setO_Code_devise_emission_Etablissement_emetteur(rgvMessage.getRGVchamps().getCodeDeviseEmission());
    result.setO_Indicateur_admission_aux_systemes_Reglement_Livraison_Etablissement_emetteur(
        rgvMessage.getRGVchamps().getIndicateurAdmissionSystemesRL()
    );

    // ===== Taux interet fixe =====
    String tauxFixeMontantStr = rgvMessage.getRGVchamps().getTauxInteretFixe().getMontant();
    double tauxFixeMontant = toDoubleSafe(tauxFixeMontantStr);

    // if(number(montant)=0) then "" else format
    result.setF_Format_Taux_interet_fixe(
        tauxFixeMontant == 0d ? "" : rgvMessage.getRGVchamps().getTauxInteretFixe().getFormat()
    );
    result.setZ_Montant_Taux_interet_fixe(
        StringUtils.leftPad(nullToEmpty(tauxFixeMontantStr), 15, '0')
    );

    String commTcn = rgvMessage.getRGVchamps().getTauxInteretFixe().getCommentaireTCN();
    if (isPresent(commTcn)) {
        result.setF_Commentaire_pour_TCN_Taux_interet_fixe(commTcn);
    }

    // ===== Montant unitaire remboursement =====
    String murMontantStr = rgvMessage.getRGVchamps().getMontantUnitaireRemboursement().getMontant();
    double murMontant = toDoubleSafe(murMontantStr);

    // if(not(contains("000-035-015-016", trim(codeTaux))) or number(montant)=0) then "" else codeDevise/format
    boolean murApplicable = containsInDashList("000-035-015-016", codeTaux) && murMontant != 0d;

    result.setF_Code_devise_Montant_unitaire_remboursement(
        murApplicable ? rgvMessage.getRGVchamps().getMontantUnitaireRemboursement().getCodeDevise() : ""
    );
    result.setF_Format_Montant_unitaire_remboursement(
        murApplicable ? rgvMessage.getRGVchamps().getMontantUnitaireRemboursement().getFormat() : ""
    );

    // if(contains("000-035-015-016", trim(codeTaux))) then pad-front(montant,15,'0') else "000000000000000"
    result.setZ_Montant_Montant_unitaire_remboursement(
        containsInDashList("000-035-015-016", codeTaux)
            ? StringUtils.leftPad(nullToEmpty(murMontantStr), 15, '0')
            : "000000000000000"
    );

    // ===== Taux marge absolue =====
    String tmaMontantStr = rgvMessage.getRGVchamps().getTauxMargeAbsolue().getMontant();
    double tmaMontant = toDoubleSafe(tmaMontantStr);

    result.setF_Code_sens_taux_Taux_marge_absolue(
        tmaMontant == 0d ? "" : rgvMessage.getRGVchamps().getTauxMargeAbsolue().getCodeSens()
    );
    result.setF_Format_Taux_marge_absolue(
        tmaMontant == 0d ? "" : rgvMessage.getRGVchamps().getTauxMargeAbsolue().getFormat()
    );
    result.setZ_Montant_Taux_marge_absolue(
        StringUtils.leftPad(nullToEmpty(tmaMontantStr), 15, '0')
    );

    // if contains("000-035", trim(codeTaux)) then value else " "
    result.setF_Code_periodicite_interets_Taux_marge_absolue(
        containsInDashList("000-035", codeTaux) ? rgvMessage.getRGVchamps().getCodePeriodiciteInterets() : " "
    );
    result.setF_Type_interets_Taux_marge_absolue(
        containsInDashList("000-035", codeTaux) ? rgvMessage.getRGVchamps().getTypeInterets() : " "
    );

    // ===== Guarantee (xsl:if) =====
    String guarantee = rgvMessage.getRGVchamps().getEtablissementEmetteur().getGarantee();
    if (isPresent(guarantee)) {
        result.setGuarantee(guarantee);
    }

    // tib:pad-front(..., '3',' ') / ('6',' ')
    result.setProgram_Originator(
        StringUtils.leftPad(nullToEmpty(rgvMessage.getRGVchamps().getEtablissementEmetteur().getProgramOrigin()), 3, ' ')
    );
    result.setProgram_Identifier(
        StringUtils.leftPad(nullToEmpty(rgvMessage.getRGVchamps().getEtablissementEmetteur().getProgramId()), 6, ' ')
    );

    // ===== Caracteristiques emission =====
    result.setO_Reference_operation_emission_Caracteristiques_emission(
        rgvMessage.getRGVchamps().getCaracteristiquesEP().getReferenceOperation()
    );

    // ===== Montant emission TCN =====
    result.setO_Code_devise_Montant_emission_TCN(
        rgvMessage.getRGVchamps().getMontantEmissionTCN().getCodeDevise()
    );
    result.setO_Montant_Montant_emission_TCN(
        StringUtils.leftPad(nullToEmpty(rgvMessage.getRGVchamps().getMontantEmissionTCN().getMontant()), 15, '0')
    );
    result.setO_Code_adherent_etablissement_debite_Montant_emission_TCN(
        rgvMessage.getRGVchamps().getMontantEmissionTCN().getCodeAdherentEtablissementDebite()
    );

    // ===== Compte titre crédité =====
    if (rgvMessage.getRGVchamps().getCompteTitreCredite() != null) {
        var ctc = rgvMessage.getRGVchamps().getCompteTitreCredite();

        if (isPresent(ctc.getZoneReservee())) {
            result.setF_Zone_reservee_Compte_titres_credite(ctc.getZoneReservee());
        }
        result.setO_Code_etablissement_Compte_titres_credite(ctc.getCodeEtablissement());
        result.setO_Type_sous_compte_Compte_titres_credite(ctc.getTypeSousCompte());
        result.setO_Numero_sous_compte_Compte_titres_credite(ctc.getNumeroSousCompte());

        if (isPresent(ctc.getZoneReservee())) {
            result.setF_Zone_reservee_Compte_titres_credite_1(ctc.getZoneReservee());
        }
        if (isPresent(ctc.getCodeValeur())) {
            result.setF_Code_valeur_Compte_titres_credite(ctc.getCodeValeur());
        }

        // tib:pad-front(codeNatureCompte, '3','0')
        result.setZ_Code_nature_compte_Compte_titres_credite(
            StringUtils.leftPad(nullToEmpty(ctc.getCodeNatureCompte()), 3, '0')
        );

        if (isPresent(ctc.getZoneReservee())) {
            result.setF_Zone_reservee_Compte_titres_credite_2(ctc.getZoneReservee());
        }
        if (isPresent(ctc.getICPG())) {
            result.setF_ICPG_Compte_titres_credite(ctc.getICPG());
        }
    }

    // ===== Compte titre débité =====
    if (rgvMessage.getRGVchamps().getCompteTitreDebite() != null) {
        var ctd = rgvMessage.getRGVchamps().getCompteTitreDebite();

        if (isPresent(ctd.getZoneReservee())) {
            result.setF_Zone_reservee_Compte_titres_debite(ctd.getZoneReservee());
        }
        result.setO_Code_etablissement_Compte_titres_debite(ctd.getCodeEtablissement());
        result.setO_Type_sous_compte_Compte_titres_debite(ctd.getTypeSousCompte());
        result.setO_Numero_sous_compte_Compte_titres_debite(ctd.getNumeroSousCompte());

        if (isPresent(ctd.getZoneReservee())) {
            result.setF_Zone_reservee_Compte_titres_debite_1(ctd.getZoneReservee());
        }
        if (isPresent(ctd.getCodeValeur())) {
            result.setF_Code_valeur_Compte_titres_debite(ctd.getCodeValeur());
        }

        result.setZ_Code_nature_compte_Compte_titres_debite(
            StringUtils.leftPad(nullToEmpty(ctd.getCodeNatureCompte()), 3, '0')
        );

        if (isPresent(ctd.getZoneReservee())) {
            result.setF_Zone_reservee_Compte_titres_debite_2(ctd.getZoneReservee());
        }
        if (isPresent(ctd.getICPG())) {
            result.setF_ICPG_Compte_titres_debite(ctd.getICPG());
        }

        result.setO_Date_denouement_theorique_Compte_titres_debite(ctd.getDateDenouementTheorique());
        result.setZ_Heure_denouement_theorique_Compte_titres_debite(ctd.getHeureDenouementTheorique());
    }

    // ===== References utilisateurs (livre) =====
    if (rgvMessage.getRGVchamps().getReferencesUtilisateurs() != null) {
        var ru = rgvMessage.getRGVchamps().getReferencesUtilisateurs();

        if (isPresent(ru.getZoneReservee())) {
            result.setF_Zone_reservee_Reference_livre(ru.getZoneReservee());
        }
        if (isPresent(ru.getCodeEtablissement())) {
            result.setF_Code_etablissement_Reference_livre(ru.getCodeEtablissement());
        }
        if (isPresent(ru.getCodeAdherent())) {
            result.setF_Code_adherent_Reference_livre(ru.getCodeAdherent());
        }
        if (isPresent(ru.getReferenceOperationEtablissement())) {
            result.setF_Reference_operation_chez_etablissement_Reference_livre(ru.getReferenceOperationEtablissement());
        }
    }

    // ===== References livreur =====
    if (rgvMessage.getRGVchamps().getReferencesLivreur() != null) {
        var rl = rgvMessage.getRGVchamps().getReferencesLivreur();

        if (isPresent(rl.getZoneReservee())) {
            result.setF_Zone_reservee_Reference_livreur(rl.getZoneReservee());
        }
        result.setO_Code_etablissement_Reference_livreur(rl.getCodeEtablissement());
        result.setO_Code_adherent_Reference_livreur(rl.getCodeAdherent());
        result.setO_Reference_operation_chez_etablissement_Reference_livreur(rl.getReferenceOperationEtablissement());

        if (isPresent(rl.getCommentaire())) {
            result.setF_Commentaire_Reference_livreur(rl.getCommentaire());
        }
    }

    // Push dans l'Exchange (comme MT065)
    exchange.getIn().setBody(result);
}

/* ===========================
   Utilitaires (comme MT065)
   =========================== */

private static boolean isPresent(String s) {
    return s != null && !s.isEmpty();
}

private static String nullToEmpty(String s) {
    return s == null ? "" : s;
}

private static String trimSafe(String s) {
    return s == null ? "" : s.trim();
}

/**
 * Reproduit le "contains('000-035-015-016', code)" du XSLT BW :
 * on considère une liste "-"-séparée, on matche sur token exact.
 */
private static boolean containsInDashList(String dashList, String code) {
    if (dashList == null || code == null) return false;
    String c = code.trim();
    if (c.isEmpty()) return false;
    // match token exact: "-CODE-" dans "-000-035-..."
    String hay = "-" + dashList + "-";
    String needle = "-" + c + "-";
    return hay.contains(needle);
}

/**
 * BW: number("") => 0
 * Ici: null/blank/invalid => 0
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
