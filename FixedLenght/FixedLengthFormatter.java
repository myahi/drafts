package com.mycompany.eai.camel.core.fixed;

import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * Fixed-length formatter (generic).
 * - No POJOs / no Bindy annotations.
 * - Produces fixed-length lines from a Map<String, ?> using a provided layout.
 */
@Component
public class FixedLengthFormatter {

    public enum Align { LEFT, RIGHT }

    /** Global formatting options (record-level). */
    public static class Options {
        private Charset charset = Charset.forName("UTF-8");
        private String lineSeparator = "\n";
        private boolean failOnUnknownFields = false; // if true: error if map contains keys not in layout
        private boolean failOnMissingRequired = true; // if true: missing required field -> error
        private boolean normalizeNewlinesInValues = true; // replace \r/\n in field values
        private char newlineReplacement = ' '; // used if normalizeNewlinesInValues=true

        public Charset getCharset() { return charset; }
        public String getLineSeparator() { return lineSeparator; }
        public boolean isFailOnUnknownFields() { return failOnUnknownFields; }
        public boolean isFailOnMissingRequired() { return failOnMissingRequired; }
        public boolean isNormalizeNewlinesInValues() { return normalizeNewlinesInValues; }
        public char getNewlineReplacement() { return newlineReplacement; }

        public Options charset(Charset charset) { this.charset = charset; return this; }
        public Options lineSeparator(String lineSeparator) { this.lineSeparator = lineSeparator; return this; }
        public Options failOnUnknownFields(boolean b) { this.failOnUnknownFields = b; return this; }
        public Options failOnMissingRequired(boolean b) { this.failOnMissingRequired = b; return this; }
        public Options normalizeNewlinesInValues(boolean b) { this.normalizeNewlinesInValues = b; return this; }
        public Options newlineReplacement(char c) { this.newlineReplacement = c; return this; }

        public static Options defaults() { return new Options(); }
    }

    /**
     * Field definition (roughly equivalent to Bindy's fixed-length field options).
     * Keep it generic: all values come from Map by "name".
     */
    public static class FieldDef {
        private final String name;
        private final int length;

        // Bindy-like options
        private Align align = Align.LEFT;
        private char paddingChar = ' ';
        private boolean trim = true;            // trim input before padding
        private boolean clip = true;            // truncate if longer than length
        private boolean required = false;       // missing -> error (depending on Options)
        private String defaultValue = "";       // used when value missing or null (if required=false)
        private String nullValue = "";          // used when explicit null
        private boolean keepEmptyAsEmpty = true;// if value=="" keep "", else use defaultValue

        // Formatting options (like pattern/precision/impliedDecimal)
        private String pattern;                 // date/number pattern (optional)
        private Integer precision;              // decimals precision for numeric output
        private RoundingMode roundingMode = RoundingMode.HALF_UP;
        private boolean impliedDecimal = false; // if true: remove decimal separator (e.g. 12.34 -> 1234)
        private Character decimalSeparator;     // optional override
        private Character groupingSeparator;    // optional override
        private Boolean forceSign;              // for numeric: always show +/-

        // Custom formatter hook (wins over pattern/precision if provided)
        private Function<Object, String> formatter;

        public FieldDef(String name, int length) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Field name is blank");
            if (length <= 0) throw new IllegalArgumentException("Field length must be > 0");
            this.name = name;
            this.length = length;
        }

        public String getName() { return name; }
        public int getLength() { return length; }
        public Align getAlign() { return align; }
        public char getPaddingChar() { return paddingChar; }
        public boolean isTrim() { return trim; }
        public boolean isClip() { return clip; }
        public boolean isRequired() { return required; }
        public String getDefaultValue() { return defaultValue; }
        public String getNullValue() { return nullValue; }
        public boolean isKeepEmptyAsEmpty() { return keepEmptyAsEmpty; }
        public String getPattern() { return pattern; }
        public Integer getPrecision() { return precision; }
        public RoundingMode getRoundingMode() { return roundingMode; }
        public boolean isImpliedDecimal() { return impliedDecimal; }
        public Character getDecimalSeparator() { return decimalSeparator; }
        public Character getGroupingSeparator() { return groupingSeparator; }
        public Boolean getForceSign() { return forceSign; }
        public Function<Object, String> getFormatter() { return formatter; }

        // Fluent setters
        public FieldDef align(Align a) { this.align = a; return this; }
        public FieldDef paddingChar(char c) { this.paddingChar = c; return this; }
        public FieldDef trim(boolean b) { this.trim = b; return this; }
        public FieldDef clip(boolean b) { this.clip = b; return this; }
        public FieldDef required(boolean b) { this.required = b; return this; }
        public FieldDef defaultValue(String v) { this.defaultValue = v == null ? "" : v; return this; }
        public FieldDef nullValue(String v) { this.nullValue = v == null ? "" : v; return this; }
        public FieldDef keepEmptyAsEmpty(boolean b) { this.keepEmptyAsEmpty = b; return this; }

        public FieldDef pattern(String p) { this.pattern = p; return this; }
        public FieldDef precision(Integer p) { this.precision = p; return this; }
        public FieldDef roundingMode(RoundingMode rm) { this.roundingMode = rm; return this; }
        public FieldDef impliedDecimal(boolean b) { this.impliedDecimal = b; return this; }
        public FieldDef decimalSeparator(Character c) { this.decimalSeparator = c; return this; }
        public FieldDef groupingSeparator(Character c) { this.groupingSeparator = c; return this; }
        public FieldDef forceSign(Boolean b) { this.forceSign = b; return this; }

        public FieldDef formatter(Function<Object, String> f) { this.formatter = f; return this; }
    }

    /** Formats a single fixed-length line (no lineSeparator appended). */
    public String formatLine(List<FieldDef> layout, Map<String, ?> values, Options options) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(options, "options");
        if (layout.isEmpty()) throw new IllegalArgumentException("layout must not be empty");

        // optional: detect unknown keys
        if (options.isFailOnUnknownFields()) {
            Set<String> allowed = new HashSet<>();
            for (FieldDef f : layout) allowed.add(f.getName());
            for (String k : values.keySet()) {
                if (!allowed.contains(k)) {
                    throw new FixedLengthFormatException("Unknown field in values map: " + k);
                }
            }
        }

        StringBuilder sb = new StringBuilder(layout.stream().mapToInt(FieldDef::getLength).sum());
        for (FieldDef f : layout) {
            Object raw = values.get(f.getName());
            String s = renderValue(raw, f, options, values.containsKey(f.getName()));
            sb.append(padAndClip(s, f));
        }
        return sb.toString();
    }

    /** Formats and writes multiple lines to an OutputStream (streaming-friendly). */
    public void writeLines(List<FieldDef> layout,
                           Iterable<Map<String, ?>> rows,
                           OutputStream out,
                           Options options) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(options, "options");

        try (Writer w = new BufferedWriter(new OutputStreamWriter(out, options.getCharset()))) {
            for (Map<String, ?> row : rows) {
                String line = formatLine(layout, row, options);
                w.write(line);
                w.write(options.getLineSeparator());
            }
            w.flush();
        } catch (Exception e) {
            throw new FixedLengthFormatException("Error writing fixed-length output", e);
        }
    }

    // ----------------- internals -----------------

    private String renderValue(Object raw,
                               FieldDef f,
                               Options options,
                               boolean keyPresent) {

        // missing key
        if (!keyPresent) {
            if (f.isRequired() && options.isFailOnMissingRequired()) {
                throw new FixedLengthFormatException("Missing required field: " + f.getName());
            }
            return f.getDefaultValue();
        }

        // explicit null
        if (raw == null) {
            if (f.isRequired() && options.isFailOnMissingRequired()) {
                throw new FixedLengthFormatException("Null value for required field: " + f.getName());
            }
            return f.getNullValue();
        }

        // custom formatter wins
        if (f.getFormatter() != null) {
            String s = f.getFormatter().apply(raw);
            return sanitize(s, options);
        }

        // base string conversion
        String s;
        if (raw instanceof CharSequence) {
            s = raw.toString();
        } else if (raw instanceof Number) {
            s = formatNumber((Number) raw, f);
        } else if (raw instanceof LocalDate) {
            s = formatTemporal(((LocalDate) raw).atStartOfDay(ZoneId.systemDefault()).toInstant(), f);
        } else if (raw instanceof LocalDateTime) {
            s = formatTemporal(((LocalDateTime) raw).atZone(ZoneId.systemDefault()).toInstant(), f);
        } else if (raw instanceof Instant) {
            s = formatTemporal((Instant) raw, f);
        } else if (raw instanceof Date) {
            s = formatTemporal(((Date) raw).toInstant(), f);
        } else if (raw instanceof TemporalAccessor) {
            s = formatTemporal((TemporalAccessor) raw, f);
        } else {
            s = raw.toString();
        }

        s = sanitize(s, options);

        if (f.isTrim()) s = s.trim();

        // empty handling
        if (s.isEmpty() && !f.isKeepEmptyAsEmpty()) {
            s = f.getDefaultValue();
        }

        return s;
    }

    private String sanitize(String s, Options options) {
        if (s == null) return "";
        if (!options.isNormalizeNewlinesInValues()) return s;
        return s.replace('\r', options.getNewlineReplacement())
                .replace('\n', options.getNewlineReplacement());
    }

    private String padAndClip(String s, FieldDef f) {
        if (s == null) s = "";

        if (s.length() > f.getLength()) {
            if (!f.isClip()) {
                throw new FixedLengthFormatException(
                        "Value too long for field " + f.getName() +
                                " (max " + f.getLength() + ", got " + s.length() + ")"
                );
            }
            // truncate
            s = s.substring(0, f.getLength());
        }

        int pad = f.getLength() - s.length();
        if (pad <= 0) return s;

        StringBuilder out = new StringBuilder(f.getLength());
        if (f.getAlign() == Align.RIGHT) {
            for (int i = 0; i < pad; i++) out.append(f.getPaddingChar());
            out.append(s);
        } else {
            out.append(s);
            for (int i = 0; i < pad; i++) out.append(f.getPaddingChar());
        }
        return out.toString();
    }

    private String formatNumber(Number n, FieldDef f) {
        BigDecimal bd = (n instanceof BigDecimal) ? (BigDecimal) n : new BigDecimal(n.toString());

        if (f.getPrecision() != null) {
            bd = bd.setScale(f.getPrecision(), f.getRoundingMode());
        }

        // If pattern provided, use DecimalFormat pattern
        if (f.getPattern() != null && !f.getPattern().isBlank()) {
            DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
            if (f.getDecimalSeparator() != null) sym.setDecimalSeparator(f.getDecimalSeparator());
            if (f.getGroupingSeparator() != null) sym.setGroupingSeparator(f.getGroupingSeparator());
            DecimalFormat df = new DecimalFormat(f.getPattern(), sym);
            df.setRoundingMode(f.getRoundingMode());
            String s = df.format(bd);

            if (Boolean.TRUE.equals(f.getForceSign()) && !s.startsWith("-") && !s.startsWith("+")) {
                s = "+" + s;
            }
            if (f.isImpliedDecimal()) {
                s = removeDecimalSeparators(s, sym.getDecimalSeparator());
            }
            return s;
        }

        // default numeric rendering
        String s = bd.toPlainString();

        if (Boolean.TRUE.equals(f.getForceSign()) && !s.startsWith("-") && !s.startsWith("+")) {
            s = "+" + s;
        }
        if (f.isImpliedDecimal()) {
            char dec = (f.getDecimalSeparator() != null) ? f.getDecimalSeparator() : '.';
            s = removeDecimalSeparators(s, dec);
        }
        return s;
    }

    private String removeDecimalSeparators(String s, char decSep) {
        // remove decimal separator and also potential grouping separators
        return s.replace(String.valueOf(decSep), "")
                .replace(",", "")
                .replace(" ", "");
    }

    private String formatTemporal(Instant instant, FieldDef f) {
        if (f.getPattern() == null || f.getPattern().isBlank()) {
            // default ISO-like
            return instant.toString();
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(f.getPattern()).withZone(ZoneId.systemDefault());
        return dtf.format(instant);
    }

    private String formatTemporal(TemporalAccessor ta, FieldDef f) {
        if (f.getPattern() == null || f.getPattern().isBlank()) {
            return ta.toString();
        }
        return DateTimeFormatter.ofPattern(f.getPattern()).format(ta);
    }
}
