public enum SrcKey { CATEGORY, SRC_APP, SRC_VL }

public final class TranscoKeys {
  public static final String CATEGORY = "category";
  public static final String SRC_APP = "srcApp";
  public static final String SRC_VL = "srcVl";
  private TranscoKeys() {}
}

private boolean matches(Transco transco,
                        Map<String, String> srcConditions,
                        Map<String, String> tgtConditions) {

    // --- SRC conditions (inchangé sur le principe)
    boolean foundSrc = false;
    for (Map.Entry<String, String> entry : srcConditions.entrySet()) {
        String key = entry.getKey();
        String expected = entry.getValue();

        switch (key) {
            case "category": {
                if (expected.equals(transco.getCategory())) {
                    foundSrc = true;
                    break;
                } else {
                    return false;
                }
            }
            case "srcApp": {
                if (expected.equals(transco.getSrcApp())) {
                    foundSrc = true;
                    break;
                } else {
                    return false;
                }
            }
            case "srcVl": {
                if (expected.equals(transco.getSrcVl())) {
                    foundSrc = true;
                    break;
                } else {
                    return false;
                }
            }
            default:
                throw new IllegalArgumentException("Unexpected value: " + key);
        }
    }

    // --- TGT conditions (nouveau) : si null ou vide => pas de filtrage sur output
    if (tgtConditions == null || tgtConditions.isEmpty()) {
        return foundSrc;
    }

    // Si la transco n'a pas d'output et qu'on demande des tgtConditions => no match
    if (transco.getOutput() == null || transco.getOutput().isEmpty()) {
        return false;
    }

    // Chaque entry de tgtConditions doit être satisfaite :
    // - key = Output.name attendu
    // - value = Output.value attendu
    // NB: on utilise une correspondance "name==key && value==expected"
    boolean foundTgt = false;

    for (Map.Entry<String, String> cond : tgtConditions.entrySet()) {
        String expectedName = cond.getKey();
        String expectedValue = cond.getValue();

        boolean thisCondOk = transco.getOutput().stream()
                .anyMatch(o -> expectedName.equals(o.getName())
                        && expectedValue.equals(o.getValue()));

        if (!thisCondOk) {
            return false; // une condition tgt manque => rejet
        } else {
            foundTgt = true; // au moins une condition tgt a matché
        }
    }

    // On matche si on a satisfait les srcConditions + tgtConditions
    // (foundTgt sera true si tgtConditions non vide)
    return foundSrc && foundTgt;
}
