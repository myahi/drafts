@Override
public Exchange aggregate(Exchange oldEx, Exchange newEx) {
    // Récupération de la ligne courante (venant du map)
    SfdhMarginCallLine newLine = newEx.getIn().getBody(SfdhMarginCallLine.class);
    List<SfdhMarginCallLine> list;

    if (oldEx == null) {
        // Initialisation de la liste pour le premier passage du groupe (codeBom)
        list = new ArrayList<>();
        list.add(newLine);
        
        // On place la liste dans le corps du message
        newEx.getIn().setBody(list);
        return newEx;
    }

    // Récupération de la liste existante pour ce codeBom
    list = oldEx.getIn().getBody(List.class);
    
    // Ajout de la nouvelle ligne à la liste du groupe
    if (newLine != null) {
        list.add(newLine);
    }

    // On retourne l'Exchange contenant la liste mise à jour
    return oldEx;
}
