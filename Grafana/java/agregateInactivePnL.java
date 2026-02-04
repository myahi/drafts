
private List<String> aggregateInactivePnL(List<String> lines) {

    final String SEP = ";";
    final String POST_CHANGE_PREFIX = "POS_CHANGE_HIST_";

    final int PNL_TYPE_IDX = 2;
    final int PRODUCT_TYPE_IDX = 4;
    final int CURRENCY_IDX = 9;
    final int BOOK_IDX = 11;

    final int SUM_FROM = 18;
    final int SUM_TO = 33;

    List<String> result = new ArrayList<>();

    // clé métier -> colonnes agrégées
    Map<String, String[]> aggregated = new LinkedHashMap<>();

    for (String line : lines) {

        String[] cols = line.split(SEP, -1);

        // Non-Inactive : on garde tel quel
        if (!"Inactive".equals(cols[PNL_TYPE_IDX])) {
            result.add(line);
            continue;
        }

        // Clé d’agrégation
        String normalizedProduct =
                StringUtils.remove(cols[PRODUCT_TYPE_IDX], POST_CHANGE_PREFIX);

        String key =
                cols[CURRENCY_IDX] + "|" +
                cols[BOOK_IDX] + "|" +
                normalizedProduct;

        // Première occurrence → init
        if (!aggregated.containsKey(key)) {
            aggregated.put(key, cols);
            continue;
        }

        // Sinon → somme des colonnes numériques
        String[] target = aggregated.get(key);

        for (int k = SUM_FROM; k <= SUM_TO; k++) {
            BigDecimal a = convertStringToBigDecimal(target[k]);
            BigDecimal b = convertStringToBigDecimal(cols[k]);
            target[k] = formatBigDecimal(a.add(b));
        }
    }

    // Ajout des Inactive agrégées au résultat final
    for (String[] cols : aggregated.values()) {
        result.add(concatColumn(cols));
    }

    return result;
}


private List<String> agregateInactivePnL(List<String> lines){
		final String postChangeHistPrefix = "POS_CHANGE_HIST_";
		String COLUMN_SEPARATOR_IN_OPL_FILE = ";";
		int pnlTypeColumnIndex = 2;
		int pnlCurrency = 9;
		int bookColumnIndex = 11;
		List<String> result = new ArrayList<String>();
		List<String> inactivePNLs = new ArrayList<String>();
		
		for (String line : lines) {
			
			String[] columns = line.split(COLUMN_SEPARATOR_IN_OPL_FILE,-1);
			if(!"Inactive".equals(columns[pnlTypeColumnIndex])){
				result.add(line);
			}
			else{
				inactivePNLs.add(line);
			}
		}
		List<String> inactivePNLsSorted = new ArrayList<String>(inactivePNLs);
		for (int i = 0; i < inactivePNLs.size(); i++) {
				if(inactivePNLs.get(i).contains("POS_CHANGE_HIST_")){
					inactivePNLsSorted.remove(i);
					inactivePNLsSorted.add(0, inactivePNLs.get(i));
				}
		}
		
		Set<String> bookAndCurrenciesSet = new HashSet<String>();
		ArrayList<String> inactivePNLsResult = new ArrayList<String>();
		for (int i = 0; i < inactivePNLsSorted.size(); i++) {
			String[] columns = inactivePNLsSorted.get(i).split(COLUMN_SEPARATOR_IN_OPL_FILE,-1);
			for (int j = 0; j < inactivePNLsSorted.size(); j++) {
				String[] currentColumns = inactivePNLsSorted.get(j).split(COLUMN_SEPARATOR_IN_OPL_FILE,-1);
				if(!columns[4].equals(currentColumns[4])){
					if (!bookAndCurrenciesSet.contains(currentColumns[pnlCurrency] + currentColumns[bookColumnIndex] + currentColumns[productTypeColumnIndex]) && currentColumns[pnlCurrency].equals(columns[pnlCurrency])
							&& currentColumns[bookColumnIndex].equals(columns[bookColumnIndex])
							&& StringUtils.remove(currentColumns[productTypeColumnIndex], postChangeHistPrefix).equals(StringUtils.remove(columns[productTypeColumnIndex], postChangeHistPrefix))) {
						for (int k = 18; k < 34; k++) {
							columns[k] = formatBigDecimal(convertStringToBigDecimal(columns[k]).add(convertStringToBigDecimal(currentColumns[k])));
						}
					}
			}
			}
			if(!bookAndCurrenciesSet.contains(columns[pnlCurrency] + columns[bookColumnIndex] + StringUtils.remove(columns[productTypeColumnIndex], postChangeHistPrefix))){
			inactivePNLsResult.add(concatColumn(columns));
			bookAndCurrenciesSet.add(columns[pnlCurrency] + columns[bookColumnIndex] + StringUtils.remove(columns[productTypeColumnIndex],postChangeHistPrefix));
			}
		}
		
		result.addAll(inactivePNLsResult);
		return result;
	}
