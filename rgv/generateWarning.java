private String generateWarning(RGVmessage rgvMessage) {

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVcommun)
	            .map(RGVcommun::getIdentificationEmetteur)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVmessage/RGVcommun/identificationEmetteur. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVcommun)
	            .map(RGVcommun::getIdentificationRecepteur)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVmessage/RGVcommun/identificationRecepteur. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVcommun)
	            .map(RGVcommun::getReferenceOperation)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVmessage/RGVcommun/referenceOperation. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVcommun)
	            .map(RGVcommun::getCodeOperation)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVmessage/RGVcommun/codeOperation. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getEtablissementEmetteur)
	            .map(EtablissementEmetteur::getCodeEtablissement)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/etablissementEmetteur/codeEtablissement. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getEtablissementEmetteur)
	            .map(EtablissementEmetteur::getTypeCode)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/etablissementEmetteur/typeCode. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodeValeur)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/codeValeur. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodeTauxTCN)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/codeTauxTCN. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodeDeviseEmission)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/codeDeviseEmission. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getIndicateurAdmissionSystemesRL)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/indicateurAdmissionSystemesRL. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCaracteristiquesEP)
	            .map(CaracteristiquesEP::getReferenceOperation)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/caracteristiquesEP/referenceOperation. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCaracteristiquesEP)
	            .map(CaracteristiquesEP::getCodeAdherentPartie)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/caracteristiquesEP/codeAdherentPartie. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCaracteristiquesEP)
	            .map(CaracteristiquesEP::getTypeSousComptePartie)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/caracteristiquesEP/typeSousComptePartie. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCaracteristiquesEP)
	            .map(CaracteristiquesEP::getNumeroSousComptePartie)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/caracteristiquesEP/numeroSousComptePartie. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodeEtablissementContrepartie)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/codeEtablissementContrepartie. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodeTypeInstruction)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/codeTypeInstruction. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getDateNegociation)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/dateNegociation. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getHeureNegociation)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/heureNegociation. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getDateDenouementTheorique)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/dateDenouementTheorique. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getHeureDenouementTheorique)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/heureDenouementTheorique. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getMontantEmissionTCN)
	            .map(MontantEmissionTCN::getCodeDevise)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/montantEmissionTCN/decise.. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getMontantEmissionTCN)
	            .map(MontantEmissionTCN::getMontant)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/montantEmissionTCN/montant.. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getMontantNetLigneInstruction)
	            .map(MontantNetLigneInstruction::getDevise)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/montantNetLigneInstruction/devise.. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getMontantNetLigneInstruction)
	            .map(MontantNetLigneInstruction::getMontant)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .isEmpty()) {
	        return "Erreur! champ obligatoire RGVchamps/montantNetLigneInstruction/montant.. \n";
	    }


	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodification)
	            .map(Codification::getCodeAdherentDomiciliataire)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .map(s -> {
	                try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
	            })
	            .filter(d -> !d.isNaN() && d != 0d)
	            .isEmpty()) {
	        return "Contenue du champ erroné :codeAdherentDomiciliataire. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodification)
	            .map(Codification::getCodeEtablissementDomiciliataire)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .map(s -> {
	                try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
	            })
	            .filter(d -> !d.isNaN() && d != 0d)
	            .isEmpty()) {
	        return "Contenue du champ erroné :codeEtablissementDomiciliataire. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCaracteristiquesEP)
	            .map(CaracteristiquesEP::getCodeAdherentPartie)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .map(s -> {
	                try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
	            })
	            .filter(d -> !d.isNaN() && d != 0d)
	            .isEmpty()) {
	        return "Contenue du champ erroné :codeAdherentPartie. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodeEtablissementContrepartie)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .map(s -> {
	                try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
	            })
	            .filter(d -> !d.isNaN() && d != 0d)
	            .isEmpty()) {
	        return "Contenue du champ erroné :codeEtablissementContrepartie. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodeValeur)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .map(s -> {
	                try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
	            })
	            .filter(d -> !d.isNaN() && d != 0d)
	            .isEmpty()) {
	        return "Contenue du champ erroné :RGVchamps/codeValeur. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodeAdherentContrepartie)
	            .map(String::trim)
	            .map(s -> {
	                if (s.isEmpty()) return Boolean.TRUE;
	                double d;
	                try { d = Double.parseDouble(s); } catch (Exception e) { d = Double.NaN; }
	                return (!Double.isNaN(d) && d != 0d);
	            })
	            .orElse(Boolean.FALSE) == Boolean.FALSE) {
	        return "Contenue du champ erroné :codeAdherentContrepartie regle R0061. . \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getMontantUnitaireRemboursement)
	            .map(mur -> {
	                String format = Optional.ofNullable(mur.getFormat()).map(String::trim).orElse("");
	                String montant = Optional.ofNullable(mur.getMontant()).map(String::trim).orElse("");
	                if (format.isEmpty() || montant.isEmpty()) return Boolean.TRUE;

	                double f; double m;
	                try { f = Double.parseDouble(format); } catch (Exception e) { f = Double.NaN; }
	                try { m = Double.parseDouble(montant); } catch (Exception e) { m = Double.NaN; }

	                return !Double.isNaN(f) && !Double.isNaN(m) && f >= 0d && f < 16d;
	            })
	            .orElse(Boolean.FALSE) == Boolean.FALSE) {
	        return "Error contenue montantUnitaireRemboursement. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getTauxInteretFixe)
	            .map(t -> {
	                String format = Optional.ofNullable(t.getFormat()).map(String::trim).orElse("");
	                String montant = Optional.ofNullable(t.getMontant()).map(String::trim).orElse("");
	                if (format.isEmpty() || montant.isEmpty()) return Boolean.TRUE;

	                double f; double m;
	                try { f = Double.parseDouble(format); } catch (Exception e) { f = Double.NaN; }
	                try { m = Double.parseDouble(montant); } catch (Exception e) { m = Double.NaN; }

	                return !Double.isNaN(f) && !Double.isNaN(m) && f >= 0d && f < 16d;
	            })
	            .orElse(Boolean.FALSE) == Boolean.FALSE) {
	        return "Contenue du champ erroné :tauxInteretFixe. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getTauxMargeAbsolue)
	            .map(t -> {
	                String format = Optional.ofNullable(t.getFormat()).map(String::trim).orElse("");
	                String montant = Optional.ofNullable(t.getMontant()).map(String::trim).orElse("");
	                if (format.isEmpty() || montant.isEmpty()) return Boolean.TRUE;

	                double f; double m;
	                try { f = Double.parseDouble(format); } catch (Exception e) { f = Double.NaN; }
	                try { m = Double.parseDouble(montant); } catch (Exception e) { m = Double.NaN; }

	                return !Double.isNaN(f) && !Double.isNaN(m) && f >= 0d && f < 16d;
	            })
	            .orElse(Boolean.FALSE) == Boolean.FALSE) {
	        return "Contenue du champ erroné :tauxMargeAbsolue. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getMontantNetLigneInstruction)
	            .map(MontantNetLigneInstruction::getMontant)
	            .map(String::trim)
	            .filter(s -> !s.isEmpty())
	            .map(s -> {
	                try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
	            })
	            .filter(d -> !d.isNaN())
	            .isEmpty()) {
	        return "Contenue du champ erroné :montantNetLigneInstruction. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodification)
	            .map(Codification::getDateDemande)
	            .map(String::trim)
	            .filter(s -> {
	                if (s.isEmpty()) return false;
	                try {
	                    java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("uuuuMMdd")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return true;
	                } catch (Exception e) {
	                    return false;
	                }
	            })
	            .isEmpty()) {
	        return "Contenue du champ erroné :dateDemande. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getDatePremiereEmission)
	            .map(String::trim)
	            .filter(s -> {
	                if (s.isEmpty()) return false;
	                try {
	                    java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("uuuuMMdd")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return true;
	                } catch (Exception e) {
	                    return false;
	                }
	            })
	            .isEmpty()) {
	        return "Contenue du champ erroné :datePremiereEmission. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getDatePremiereJouissance)
	            .map(s0 -> s0 == null ? "" : s0.trim())
	            .map(s -> {
	                if (s.isEmpty()) return Boolean.TRUE;
	                try {
	                    java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("uuuuMMdd")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return Boolean.TRUE;
	                } catch (Exception e) {
	                    return Boolean.FALSE;
	                }
	            })
	            .orElse(Boolean.FALSE) == Boolean.FALSE) {
	        return "Contenue du champ erroné :datePremiereJouissance. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getDatePremierPaiementInterets)
	            .map(s0 -> s0 == null ? "" : s0.trim())
	            .map(s -> {
	                if (s.isEmpty()) return Boolean.TRUE;
	                try {
	                    java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("uuuuMMdd")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return Boolean.TRUE;
	                } catch (Exception e) {
	                    return Boolean.FALSE;
	                }
	            })
	            .orElse(Boolean.FALSE) == Boolean.FALSE) {
	        return "Contenue du champ erroné :datePremierPaiementInterets. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getDateRemboursementTCN)
	            .map(String::trim)
	            .filter(s -> {
	                if (s.isEmpty()) return false;
	                try {
	                    java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("uuuuMMdd")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return true;
	                } catch (Exception e) {
	                    return false;
	                }
	            })
	            .isEmpty()) {
	        return "Contenue du champ erroné :dateRemboursementTCN. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getDateNegociation)
	            .map(String::trim)
	            .filter(s -> {
	                if (s.isEmpty()) return false;
	                try {
	                    java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("uuuuMMdd")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return true;
	                } catch (Exception e) {
	                    return false;
	                }
	            })
	            .isEmpty()) {
	        return "Contenue du champ erroné :dateNegociation. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getDateDenouementTheorique)
	            .map(String::trim)
	            .filter(s -> {
	                if (s.isEmpty()) return false;
	                try {
	                    java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("uuuuMMdd")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return true;
	                } catch (Exception e) {
	                    return false;
	                }
	            })
	            .isEmpty()) {
	        return "Contenue du champ erroné :dateDenouementTheorique. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getHeureNegociation)
	            .map(s0 -> s0 == null ? "" : s0.trim())
	            .map(s -> {
	                if (s.isEmpty()) return Boolean.TRUE;
	                try {
	                    java.time.LocalTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("HHmmss")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return Boolean.TRUE;
	                } catch (Exception e) {
	                    return Boolean.FALSE;
	                }
	            })
	            .orElse(Boolean.FALSE) == Boolean.FALSE) {
	        return "Contenue du champ erroné :heureNegociation. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getHeureDenouementTheorique)
	            .map(s0 -> s0 == null ? "" : s0.trim())
	            .map(s -> {
	                if (s.isEmpty()) return Boolean.TRUE;
	                try {
	                    java.time.LocalTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("HHmmss")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return Boolean.TRUE;
	                } catch (Exception e) {
	                    return Boolean.FALSE;
	                }
	            })
	            .orElse(Boolean.FALSE) == Boolean.FALSE) {
	        return "Contenue du champ erroné :heureDenouementTheorique. \n";
	    }

	    if (Optional.ofNullable(rgvMessage)
	            .map(RGVmessage::getRGVchamps)
	            .map(RGVchamps::getCodification)
	            .map(Codification::getHeureDemande)
	            .map(s0 -> s0 == null ? "" : s0.trim())
	            .map(s -> {
	                if (s.isEmpty()) return Boolean.TRUE;
	                try {
	                    java.time.LocalTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("HHmmss")
	                            .withResolverStyle(java.time.format.ResolverStyle.STRICT));
	                    return Boolean.TRUE;
	                } catch (Exception e) {
	                    return Boolean.FALSE;
	                }
	            })
	            .orElse(Boolean.FALSE) == Boolean.FALSE) {
	        return "Contenue du champ erroné :heureDemande. \n";
	    }
	    return "";
	}
