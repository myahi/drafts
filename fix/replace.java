20260123-00:00:03.388

  String replaceHeaderField(String fixContent){
		String result =StringUtils.replace(fixContent, StringUtils.substringBetween(fix, "|9=","|"),"XX"); 
		result = StringUtils.replace(result, StringUtils.substringBetween(fix, "|10=","|"),"XX");
		result = StringUtils.replace(result, StringUtils.substringBetween(fix, "|34=","|"),"XX");
		String tag52 = StringUtils.substringBetween(fix, "|52=","|");
		System.out.println(tag52);
		result = result.replaceAll(tag52, "XX");
		return result;
