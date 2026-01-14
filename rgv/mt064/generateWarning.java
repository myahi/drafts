import java.util.Optional;

private String generateWarning(RGVmessage rgvMessage) {

    // --- Obligatoires (ordre EXACT BW) ---

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVcommun)
            .map(RGVcommun::getIdentificationEmetteur)
            .orElse(null))) {
        return "Erreur! champ obligatoire identificationEmetteur. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVcommun)
            .map(RGVcommun::getIdentificationRecepteur)
            .orElse(null))) {
        return "Erreur! champ obligatoire identificationRecepteur. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVcommun)
            .map(RGVcommun::getReferenceOperation)
            .orElse(null))) {
        return "Erreur! champ obligatoire referenceOperation. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVcommun)
            .map(RGVcommun::getCodeOperation)
            .orElse(null))) {
        return "Erreur! champ obligatoire codeOperation. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCodification)
            .map(Codification::getDateDemande)
            .orElse(null))) {
        return "Erreur! champ obligatoire codification / dateDemande. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCodification)
            .map(Codification::getCodeAdherentDomiciliataire)
            .orElse(null))) {
        return "Erreur! champ obligatoire codification / codeAdherentDomiciliataire. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCodification)
            .map(Codification::getCodeEtablissementDomiciliataire)
            .orElse(null))) {
        return "Erreur! champ obligatoire codification / codeEtablissementDomiciliataire. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getEtablissementEmetteur)
            .map(EtablissementEmetteur::getCodeEtablissement)
            .orElse(null))) {
        return "Erreur! champ obligatoire etablissementEmetteur / codeEtablissement. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getEtablissementEmetteur)
            .map(EtablissementEmetteur::getTypeCode)
            .orElse(null))) {
        return "Erreur! champ obligatoire etablissementEmetteur / typeCode. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getTypeValeurTCN)
            .orElse(null))) {
        return "Erreur! champ obligatoire typeValeurTCN. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getDateRemboursementTCN)
            .orElse(null))) {
        return "Erreur! champ obligatoire dateRemboursementTCN. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getModaliteEmissionTCN)
            .orElse(null))) {
        return "Erreur! champ obligatoire modaliteEmissionTCN. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getDatePremiereEmission)
            .orElse(null))) {
        return "Erreur! champ obligatoire datePremiereEmission. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCodeTauxTCN)
            .orElse(null))) {
        return "Erreur! champ obligatoire codeTauxTCN. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCodeDeviseEmission)
            .orElse(null))) {
        return "Erreur! champ obligatoire codeDeviseEmission. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getIndicateurAdmissionSystemesRL)
            .orElse(null))) {
        return "Erreur! champ obligatoire indicateurAdmissionSystemesRL. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCaracteristiquesEP)
            .map(CaracteristiquesEP::getReferenceOperation)
            .orElse(null))) {
        return "Erreur! champ obligatoire caracteristiquesEP / referenceOperation. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getMontantEmissionTCN)
            .map(MontantEmissionTCN::getCodeAdherentEtablissementDebite)
            .orElse(null))) {
        return "Erreur! champ obligatoire montantEmissionTCN / codeAdherentEtablissementDebite. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getMontantEmissionTCN)
            .map(MontantEmissionTCN::getCodeDevise)
            .orElse(null))) {
        return "Erreur! champ obligatoire montantEmissionTCN / codeDevise. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getMontantEmissionTCN)
            .map(MontantEmissionTCN::getMontant)
            .orElse(null))) {
        return "Erreur! champ obligatoire montantEmissionTCN / montant. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCompteTitreCredite)
            .map(CompteTitreCredite::getCodeEtablissement)
            .orElse(null))) {
        return "Erreur! champ obligatoire compteTitreCredite / codeEtablissement. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCompteTitreCredite)
            .map(CompteTitreCredite::getTypeSousCompte)
            .orElse(null))) {
        return "Erreur! champ obligatoire compteTitreCredite / typeSousCompte. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCompteTitreCredite)
            .map(CompteTitreCredite::getNumeroSousCompte)
            .orElse(null))) {
        return "Erreur! champ obligatoire compteTitreCredite / numeroSousCompte. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCompteTitreDebite)
            .map(CompteTitreDebite::getCodeEtablissement)
            .orElse(null))) {
        return "Erreur! champ obligatoire compteTitreDebite / codeEtablissement. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCompteTitreDebite)
            .map(CompteTitreDebite::getTypeSousCompte)
            .orElse(null))) {
        return "Erreur! champ obligatoire compteTitreDebite / typeSousCompte. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getCompteTitreDebite)
            .map(CompteTitreDebite::getNumeroSousCompte)
            .orElse(null))) {
        return "Erreur! champ obligatoire compteTitreDebite / numeroSousCompte. \n";
    }

    // BW règle: RGVchamps/dateDenouementTheorique (pas dans compteTitreDebite)
    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getDateDenouementTheorique)
            .orElse(null))) {
        return "Erreur! champ obligatoire dateDenouementTheorique. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getReferencesLivreur)
            .map(ReferencesLivreur::getCodeAdherent)
            .orElse(null))) {
        return "Erreur! champ obligatoire referencesLivreur / codeAdherent. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getReferencesLivreur)
            .map(ReferencesLivreur::getReferenceOperationEtablissement)
            .orElse(null))) {
        return "Erreur! champ obligatoire referencesLivreur / referenceOperationEtablissement. \n";
    }

    if (isBlankTrimmed(Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getReferencesLivreur)
            .map(ReferencesLivreur::getCodeEtablissement)
            .orElse(null))) {
        return "Erreur! champ obligatoire referencesLivreur / codeEtablissement. \n";
    }

    return "";
}

private static boolean isBlankTrimmed(String s) {
    return s == null || s.trim().isEmpty();
}
