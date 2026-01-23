
import java.util.regex.Pattern;

public class FixMasker {

    // remplace la valeur des tags 9,10,34,52 par XX, séparateur |
    public static String maskTags(String fix) {
        if (fix == null || fix.isEmpty()) return fix;

        // 1) cas "en début de chaîne"
        fix = fix.replaceFirst("^(9|10|34|52)=[^|]*\\|", "$1=XX|");

        // 2) cas "au milieu" (précédé par |)
        fix = fix.replaceAll("\\|(9|10|34|52)=[^|]*\\|", "|$1=XX|");

        // 3) cas "en fin de chaîne" (si le message ne se termine pas par |)
        fix = fix.replaceAll("\\|(9|10|34|52)=[^|]*$", "|$1=XX");

        return fix;
    }
}


52=2026012XX-00:00:0XX.XX88
20260123-00:00:03.388

  String replaceHeaderField(String fixContent){
		String result =StringUtils.replace(fixContent, StringUtils.substringBetween(fix, "|9=","|"),"XX"); 
		result = StringUtils.replace(result, StringUtils.substringBetween(fix, "|10=","|"),"XX");
		result = StringUtils.replace(result, StringUtils.substringBetween(fix, "|34=","|"),"XX");
		String tag52 = StringUtils.substringBetween(fix, "|52=","|");
		System.out.println(tag52);
		result = result.replaceAll(tag52, "XX");
		return result;
