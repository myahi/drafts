import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuotedValueExtractor {

    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

    public static List<String> extract(String input) {
        List<String> values = new ArrayList<>();
        if (input == null) return values;

        Matcher m = QUOTED.matcher(input);
        while (m.find()) {
            values.add(m.group(1));
        }
        return values;
    }
}
