
private List<String> aggregateInactivePnL(List<String> lines) {
    final String POST_CHANGE_HIST_PREFIX = "POS_CHANGE_HIST_";
    final String SEP = ";";

    final int PNL_TYPE_IDX = 2;
    final int PRODUCT_TYPE_IDX = 4;      // chez toi c'est columns[4]
    final int CURRENCY_IDX = 9;
    final int BOOK_IDX = 11;

    final int SUM_FROM = 18;
    final int SUM_TO_EXCL = 34;          // 18..33 inclus

    List<String> result = new ArrayList<>();
    List<String> inactive = new ArrayList<>();

    // 1) split inactive / autres
    for (String line : lines) {
        String[] cols = line.split(SEP, -1);
        if (!"Inactive".equals(cols[PNL_TYPE_IDX])) {
            result.add(line);
        } else {
            inactive.add(line);
        }
    }

    // 2) ordre : POS_CHANGE_HIST_ d'abord
    List<String> inactiveSorted = new ArrayList<>(inactive);
    inactiveSorted.sort((a, b) -> {
        boolean ap = a.contains(POST_CHANGE_HIST_PREFIX);
        boolean bp = b.contains(POST_CHANGE_HIST_PREFIX);
        if (ap == bp) return 0;
        return ap ? -1 : 1;
    });

    // 3) agrégation par clé (LinkedHashMap = conserve l’ordre d’insertion)
    Map<String, String[]> agg = new LinkedHashMap<>();

    for (String line : inactiveSorted) {
        String[] cols = line.split(SEP, -1);

        String productNorm = removePrefix(cols[PRODUCT_TYPE_IDX], POST_CHANGE_HIST_PREFIX);
        String key = cols[CURRENCY_IDX] + "|" + cols[BOOK_IDX] + "|" + productNorm;

        String[] acc = agg.get(key);
        if (acc == null) {
            // première occurrence => on la garde telle quelle (mais productType normalisé si tu veux)
            // si tu veux conserver le productType original (avec prefix), ne touche pas cols[PRODUCT_TYPE_IDX]
            agg.put(key, cols);
        } else {
            // addition des montants
            for (int k = SUM_FROM; k < SUM_TO_EXCL; k++) {
                acc[k] = formatBigDecimal(
                    convertStringToBigDecimal(acc[k]).add(convertStringToBigDecimal(cols[k]))
                );
            }
        }
    }

    // 4) rebuild lignes
    for (String[] cols : agg.values()) {
        result.add(concatColumn(cols));
    }

    return result;
}

private String removePrefix(String s, String prefix) {
    if (s == null) return "";
    return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
}


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
