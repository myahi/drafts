public Exchange aggregate(Exchange oldEx, Exchange newEx) {
    SfdhMarginCallLine newLine = newEx.getIn().getBody(SfdhMarginCallLine.class);
    List<SfdhMarginCallLine> list;

    if (oldEx == null) {
        list = new ArrayList<>();
        list.add(newLine);
        newEx.getIn().setBody(list);
        return newEx;
    }

    list = oldEx.getIn().getBody(List.class);
    // Ici, vous pouvez soit ajouter la ligne, 
    // soit chercher si le codeBom existe déjà pour faire la somme (votre logique métier)
    list.add(newLine); 
    return oldEx;
}
