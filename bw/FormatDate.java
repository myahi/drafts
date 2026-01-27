import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DateColumnFormatter {

    public static String formatLine(
            String line,
            String separator,
            String[] configs
    ) {
        String[] columns = line.split(separator, -1);

        // Préparation des configs
        Map<Integer, DateFormatConfig> configMap = new HashMap<>();

        for (String config : configs) {
            String[] parts = config.split(":");
            int index = Integer.parseInt(parts[0]);
            DateTimeFormatter sourceFmt = DateTimeFormatter.ofPattern(parts[1]);
            DateTimeFormatter targetFmt = DateTimeFormatter.ofPattern(parts[2]);

            configMap.put(index, new DateFormatConfig(sourceFmt, targetFmt));
        }

        // Traitement des colonnes
        for (Map.Entry<Integer, DateFormatConfig> entry : configMap.entrySet()) {
            int index = entry.getKey();

            if (index < columns.length && !columns[index].isEmpty()) {
                try {
                    LocalDateTime date = LocalDateTime.parse(
                            columns[index],
                            entry.getValue().sourceFormat
                    );
                    columns[index] = date.format(entry.getValue().targetFormat);
                } catch (Exception e) {
                    // Si parsing échoue, on laisse la valeur telle quelle
                }
            }
        }

        return String.join(separator, columns);
    }

    // Classe interne pour stocker la conf
    private static class DateFormatConfig {
        DateTimeFormatter sourceFormat;
        DateTimeFormatter targetFormat;

        DateFormatConfig(DateTimeFormatter sourceFormat, DateTimeFormatter targetFormat) {
            this.sourceFormat = sourceFormat;
            this.targetFormat = targetFormat;
        }
    }
}
