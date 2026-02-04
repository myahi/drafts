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
