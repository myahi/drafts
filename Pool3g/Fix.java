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
