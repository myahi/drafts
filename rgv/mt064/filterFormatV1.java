private boolean filterFormat(RGVmessage rgvMessage, Map<String, Transcos> transcosByCategory) {

	    int typeRemunerationLen = Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getTypeRemuneration)
	            .map(String::trim)
	            .map(String::length)
	            .orElse(0);

	    if (typeRemunerationLen == 0 && transcoResolver.findTransco(transcosByCategory,"rgv.mt065.typeRemuneration.filter",Map.of("srcApp", "Calypso", "srcVl", "YES")) != null) {
	        return true;
	    }

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

	    int programIdLen = Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getEtablissementEmetteur)
	            .map(EtablissementEmetteur::getProgramId)
	            .map(String::trim)
	            .map(String::length)
	            .orElse(0);

	    if ((programIdLen == 0 && transcoResolver.findTransco(transcosByCategory,"rgv.mt065.programId.filter",Map.of("srcApp", "Calypso", "srcVl", "YES")) != null)|| programIdLen > 6) {
	        return true;
	    }

	    int programOriginLen = Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getEtablissementEmetteur)
	            .map(EtablissementEmetteur::getProgramOrigin)
	            .map(String::trim)
	            .map(String::length)
	            .orElse(0);

	    if ((programOriginLen == 0 && transcoResolver.findTransco(transcosByCategory,"rgv.mt065.programOrigin.filter",Map.of("srcApp", "Calypso", "srcVl", "YES")) != null) || programOriginLen > 3) {
	        return true;
	    }

	    return false;
	}
