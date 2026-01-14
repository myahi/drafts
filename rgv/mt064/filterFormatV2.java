private boolean filterFormat(RGVmessage rgvMessage, Map<String, Transcos> transcosByCategory) {

    // 1) (len(typeRemuneration)=0 AND transco(mt064.typeRemuneration.filter, Calypso, YES))
    int typeRemunerationLen = Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getTypeRemuneration)
            .map(String::trim)
            .map(String::length)
            .orElse(0);

    if (typeRemunerationLen == 0
            && transcoResolver.findTransco(
                    transcosByCategory,
                    "rgv.mt064.typeRemuneration.filter",
                    Map.of("srcApp", "Calypso", "srcVl", "YES")
            ) != null) {
        return true;
    }

    // 2) (len(garantee)=0 OR len(garantee)>1)
    int garanteeLen = Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getEtablissementEmetteur)
            .map(EtablissementEmetteur::getGarantee)
            .map(String::trim)
            .map(String::length)
            .orElse(0);

    if (garanteeLen == 0 || garanteeLen > 1) {
        return true;
    }

    // 3) ((len(programId)=0 AND transco(mt064.programId.filter, Calypso, YES)) OR len(programId)>6)
    int programIdLen = Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getEtablissementEmetteur)
            .map(EtablissementEmetteur::getProgramId)
            .map(String::trim)
            .map(String::length)
            .orElse(0);

    if ((programIdLen == 0
            && transcoResolver.findTransco(
                    transcosByCategory,
                    "rgv.mt064.programId.filter",
                    Map.of("srcApp", "Calypso", "srcVl", "YES")
            ) != null)
            || programIdLen > 6) {
        return true;
    }

    // 4) ((len(programOrigin)=0 AND transco(mt064.programOrigin.filter, Calypso, YES)) OR len(programOrigin)>3)
    int programOriginLen = Optional.ofNullable(rgvMessage)
            .map(RGVmessage::getRGVchamps)
            .map(RGVchamps::getEtablissementEmetteur)
            .map(EtablissementEmetteur::getProgramOrigin)
            .map(String::trim)
            .map(String::length)
            .orElse(0);

    if ((programOriginLen == 0
            && transcoResolver.findTransco(
                    transcosByCategory,
                    "rgv.mt064.programOrigin.filter",
                    Map.of("srcApp", "Calypso", "srcVl", "YES")
            ) != null)
            || programOriginLen > 3) {
        return true;
    }

    return false;
}
