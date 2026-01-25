package com.mycompany.eai.camel.core.fixed;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component
public class FixedLengthChunkReader {

    // Définition d’un champ fixed-length (mode "par nom")
    public static class FieldDef {
        private final String name;
        private final int length;
        private final boolean trim;

        public FieldDef(String name, int length) {
            this(name, length, true);
        }

        public FieldDef(String name, int length, boolean trim) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Field name is blank");
            if (length <= 0) throw new IllegalArgumentException("Field length must be > 0");
            this.name = name;
            this.length = length;
            this.trim = trim;
        }

        public String getName() { return name; }
        public int getLength() { return length; }
        public boolean isTrim() { return trim; }
    }

    public static class Options {
        private Charset charset = Charset.forName("UTF-8");
        private boolean strictLineLength = false; // si true : erreur si ligne trop courte

        public Charset getCharset() { return charset; }
        public boolean isStrictLineLength() { return strictLineLength; }

        public Options charset(Charset charset) { this.charset = charset; return this; }
        public Options strictLineLength(boolean strict) { this.strictLineLength = strict; return this; }

        public static Options defaults() { return new Options(); }
    }

    /* ============================================================
       MODE INDEX : int[] lengths => Map<Integer,String>
       ============================================================ */

    public void readChunksAsIndexMap(
            InputStream input,
            Options options,
            int[] lengths,
            int chunkSize,
            Consumer<List<Map<Integer, String>>> onChunk
    ) {
        Objects.requireNonNull(onChunk, "onChunk");
        readChunksAsIndexMap(input, options, lengths, chunkSize, null, (g, c) -> onChunk.accept(c));
    }

    // groupBy non trié, IN-MEMORY : groupByIndexes = ex [0,2]
    public void readChunksAsIndexMap(
            InputStream input,
            Options options,
            int[] lengths,
            int chunkSize,
            List<Integer> groupByIndexes,
            BiConsumer<String, List<Map<Integer, String>>> onChunk
    ) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(lengths, "lengths");
        Objects.requireNonNull(onChunk, "onChunk");
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize must be > 0");
        if (lengths.length == 0) throw new IllegalArgumentException("lengths must not be empty");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, options.getCharset()))) {

            boolean useGroupBy = groupByIndexes != null && !groupByIndexes.isEmpty();

            if (!useGroupBy) {
                List<Map<Integer, String>> chunk = new ArrayList<>(chunkSize);
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    chunk.add(parseIndexLine(line, lengths, options.isStrictLineLength()));
                    if (chunk.size() >= chunkSize) {
                        onChunk.accept(null, chunk);
                        chunk = new ArrayList<>(chunkSize);
                    }
                }
                if (!chunk.isEmpty()) onChunk.accept(null, chunk);
                return;
            }

            // groupBy in-memory (non trié)
            Map<String, List<Map<Integer, String>>> groups = new LinkedHashMap<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                Map<Integer, String> row = parseIndexLine(line, lengths, options.isStrictLineLength());
                String gk = buildGroupKeyByIndex(row, groupByIndexes);
                groups.computeIfAbsent(gk, k -> new ArrayList<>()).add(row);
            }

            emitGrouped(groups, chunkSize, onChunk);

        } catch (Exception e) {
            throw new FixedLengthParseException("Error reading fixed-length stream (index mode)", e);
        }
    }

    /* ============================================================
       MODE NOM : List<FieldDef> => Map<String,String>
       ============================================================ */

    public void readChunksAsFieldMap(
            InputStream input,
            Options options,
            List<FieldDef> layout,
            int chunkSize,
            Consumer<List<Map<String, String>>> onChunk
    ) {
        Objects.requireNonNull(onChunk, "onChunk");
        readChunksAsFieldMap(input, options, layout, chunkSize, null, (g, c) -> onChunk.accept(c));
    }

    // groupBy non trié, IN-MEMORY : groupByFields = ex ["ACCOUNT","DATE"]
    public void readChunksAsFieldMap(
            InputStream input,
            Options options,
            List<FieldDef> layout,
            int chunkSize,
            List<String> groupByFields,
            BiConsumer<String, List<Map<String, String>>> onChunk
    ) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(onChunk, "onChunk");
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize must be > 0");
        if (layout.isEmpty()) throw new IllegalArgumentException("layout must not be empty");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, options.getCharset()))) {

            boolean useGroupBy = groupByFields != null && !groupByFields.isEmpty();

            if (!useGroupBy) {
                List<Map<String, String>> chunk = new ArrayList<>(chunkSize);
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    chunk.add(parseNamedLine(line, layout, options.isStrictLineLength()));
                    if (chunk.size() >= chunkSize) {
                        onChunk.accept(null, chunk);
                        chunk = new ArrayList<>(chunkSize);
                    }
                }
                if (!chunk.isEmpty()) onChunk.accept(null, chunk);
                return;
            }

            Map<String, List<Map<String, String>>> groups = new LinkedHashMap<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                Map<String, String> row = parseNamedLine(line, layout, options.isStrictLineLength());
                String gk = buildGroupKeyByName(row, groupByFields);
                groups.computeIfAbsent(gk, k -> new ArrayList<>()).add(row);
            }

            emitGrouped(groups, chunkSize, onChunk);

        } catch (Exception e) {
            throw new FixedLengthParseException("Error reading fixed-length stream (named mode)", e);
        }
    }

    /* ===================== Helpers ===================== */

    private Map<Integer, String> parseIndexLine(String line, int[] lengths, boolean strict) {
        Map<Integer, String> row = new LinkedHashMap<>(lengths.length);
        int pos = 0;
        for (int i = 0; i < lengths.length; i++) {
            int len = lengths[i];
            if (len <= 0) throw new IllegalArgumentException("lengths[" + i + "] must be > 0");
            int end = pos + len;

            if (end > line.length()) {
                if (strict) {
                    throw new IllegalArgumentException("Line too short for field index " + i + " (need " + end + ", got " + line.length() + ")");
                }
                // padding with empty when line is shorter
                row.put(i, pos < line.length() ? line.substring(pos) : "");
                // remaining fields -> ""
                for (int j = i + 1; j < lengths.length; j++) row.put(j, "");
                return row;
            }

            row.put(i, line.substring(pos, end).trim());
            pos = end;
        }
        return row;
    }

    private Map<String, String> parseNamedLine(String line, List<FieldDef> layout, boolean strict) {
        Map<String, String> row = new LinkedHashMap<>(layout.size());
        int pos = 0;

        for (FieldDef f : layout) {
            int end = pos + f.getLength();

            if (end > line.length()) {
                if (strict) {
                    throw new IllegalArgumentException("Line too short for field " + f.getName() + " (need " + end + ", got " + line.length() + ")");
                }
                String v = pos < line.length() ? line.substring(pos) : "";
                row.put(f.getName(), f.isTrim() ? v.trim() : v);
                // remaining -> ""
                for (int i = layout.indexOf(f) + 1; i < layout.size(); i++) {
                    row.put(layout.get(i).getName(), "");
                }
                return row;
            }

            String v = line.substring(pos, end);
            row.put(f.getName(), f.isTrim() ? v.trim() : v);
            pos = end;
        }

        return row;
    }

    private String buildGroupKeyByName(Map<String, String> row, List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (String f : fields) {
            if (sb.length() > 0) sb.append('|');
            sb.append(f).append('=').append(Objects.toString(row.get(f), ""));
        }
        return sb.toString();
    }

    private String buildGroupKeyByIndex(Map<Integer, String> row, List<Integer> indexes) {
        StringBuilder sb = new StringBuilder();
        for (Integer idx : indexes) {
            if (sb.length() > 0) sb.append('|');
            sb.append(idx).append('=').append(Objects.toString(row.get(idx), ""));
        }
        return sb.toString();
    }

    private <T> void emitGrouped(Map<String, List<T>> groups, int chunkSize, BiConsumer<String, List<T>> onChunk) {
        for (Map.Entry<String, List<T>> e : groups.entrySet()) {
            String gk = e.getKey();
            List<T> rows = e.getValue();
            for (int i = 0; i < rows.size(); i += chunkSize) {
                onChunk.accept(gk, rows.subList(i, Math.min(i + chunkSize, rows.size())));
            }
        }
    }
}
