@Override
public Exchange aggregate(Exchange oldEx, Exchange newEx) {
    SfdhMarginCallLine newLine = newEx.getIn().getBody(SfdhMarginCallLine.class);
    
    if (oldEx == null) {
        // Initialisation : On crée une liste pour stocker nos lignes uniques par codeBom
        List<SfdhMarginCallLine> groupedList = new ArrayList<>();
        groupedList.add(newLine);
        newEx.getIn().setBody(groupedList);
        return newEx;
    }

    List<SfdhMarginCallLine> currentList = oldEx.getIn().getBody(List.class);

    // On cherche si une ligne avec le même codeBom existe déjà dans notre accumulation
    Optional<SfdhMarginCallLine> existingLine = currentList.stream()
            .filter(l -> l.getCODE_BOM().equals(newLine.getCODE_BOM()))
            .findFirst();

    if (existingLine.isPresent()) {
        // MATCH : On additionne les montants BigDecimal
        SfdhMarginCallLine match = existingLine.get();
        match.nominalValue = match.nominalValue.add(newLine.nominalValue);
        match.nominalEuroValue = match.nominalEuroValue.add(newLine.nominalEuroValue);
    } else {
        // PAS DE MATCH : On ajoute cette nouvelle ligne de codeBom à la liste
        currentList.add(newLine);
    }

    return oldEx;
}




public Exchange aggregate(Exchange oldEx, Exchange newEx) {
    if (oldEx == null) {
        return newEx;
    }

    SfdhMarginCallLine oldLine = oldEx.getIn().getBody(SfdhMarginCallLine.class);
    SfdhMarginCallLine newLine = newEx.getIn().getBody(SfdhMarginCallLine.class);

    // Somme des montants BigDecimal (champs transient)
    if (newLine != null && oldLine != null) {
        oldLine.nominalValue = oldLine.nominalValue.add(newLine.nominalValue);
        oldLine.nominalEuroValue = oldLine.nominalEuroValue.add(newLine.nominalEuroValue);
    }

    return oldEx;
}



public void finalizeGroup(SfdhMarginCallLine line) {
    if (line != null) {
        // Conversion des BigDecimal cumulés en String avec virgule pour le CSV
        line.setNOMINAL(formatWithComma(line.nominalValue));
        line.setNOMINAL_EURO(formatWithComma(line.nominalEuroValue));
    }
}
