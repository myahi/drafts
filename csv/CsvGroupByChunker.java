

package com.mycompany.eai.camel.core.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
public class CsvGroupByChunker {

    private static final String GRP_KEY_COL = "__grpkey";

    public void groupByColumnsChunked(
            InputStream input,
            List<String> groupByColumns,
            int chunkSize,
            Path tmpDir,
            BiConsumer<String, List<Map<String, String>>> onChunk
    ) {
        Path tmpFile = null;

        // CSVFormat: utiliser le builder + get() (sans withXxx() ni build()).
        final CSVFormat inFormat = CSVFormat.DEFAULT
                .builder()
                .setTrim(true)
                .get();

        final CSVFormat outFormat = CSVFormat.DEFAULT
                .builder()
                .setTrim(true)
                .get();

        try {
            Files.createDirectories(tmpDir);
            tmpFile = Files.createTempFile(tmpDir, "csvgrp-", ".csv");

            // Pass 1 : lire l'en-tête (première ligne) manuellement + écrire tout avec clé de groupe
            try (BufferedReader r = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                 CSVParser p = CSVParser.parse(r, inFormat);
                 BufferedWriter w = Files.newBufferedWriter(tmpFile, StandardCharsets.UTF_8);
                 CSVPrinter out = new CSVPrinter(w, outFormat)) {

                var it = p.iterator();
                if (!it.hasNext()) {
                    // CSV vide
                    return;
                }

                CSVRecord headerRec = it.next();
                List<String> headers = new ArrayList<>(headerRec.size());
                for (int i = 0; i < headerRec.size(); i++) {
                    headers.add(headerRec.get(i));
                }

                // Ecrire header + colonne technique
                List<String> outHeaders = new ArrayList<>(headers);
                outHeaders.add(GRP_KEY_COL);
                out.printRecord(outHeaders);

                // Ecrire les records
                while (it.hasNext()) {
                    CSVRecord rec = it.next();

                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        String h = headers.get(i);
                        String v = (i < rec.size()) ? rec.get(i) : "";
                        row.put(h, v);
                    }

                    String groupKey = buildGroupKey(row, groupByColumns);

                    List<String> values = new ArrayList<>(headers.size() + 1);
                    for (String h : headers) {
                        values.add(row.getOrDefault(h, ""));
                    }
                    values.add(groupKey);

                    out.printRecord(values);
                }

                out.flush();
            }

            // Pass 2 : regroupement en mémoire (par groupe), en relisant le fichier temporaire
            Map<String, List<Map<String, String>>> groups = new LinkedHashMap<>();

            try (BufferedReader r = Files.newBufferedReader(tmpFile, StandardCharsets.UTF_8);
                 CSVParser p = CSVParser.parse(r, inFormat)) {

                var it = p.iterator();
                if (!it.hasNext()) {
                    return;
                }

                CSVRecord headerRec = it.next();
                List<String> headers = new ArrayList<>(headerRec.size());
                int grpKeyIdx = -1;

                for (int i = 0; i < headerRec.size(); i++) {
                    String h = headerRec.get(i);
                    headers.add(h);
                    if (GRP_KEY_COL.equals(h)) {
                        grpKeyIdx = i;
                    }
                }

                if (grpKeyIdx < 0) {
                    throw new CsvGroupByException("Missing technical column " + GRP_KEY_COL + " in tmp file header");
                }

                while (it.hasNext()) {
                    CSVRecord rec = it.next();
                    String groupKey = (grpKeyIdx < rec.size()) ? rec.get(grpKeyIdx) : "";

                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        if (i == grpKeyIdx) continue;
                        String h = headers.get(i);
                        String v = (i < rec.size()) ? rec.get(i) : "";
                        row.put(h, v);
                    }

                    groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(row);
                }
            }

            // Emit par chunks
            for (Map.Entry<String, List<Map<String, String>>> e : groups.entrySet()) {
                List<Map<String, String>> rows = e.getValue();
                for (int i = 0; i < rows.size(); i += chunkSize) {
                    onChunk.accept(
                            e.getKey(),
                            rows.subList(i, Math.min(i + chunkSize, rows.size()))
                    );
                }
            }

        } catch (Exception e) {
            throw new CsvGroupByException("CSV groupBy error", e);
        } finally {
            if (tmpFile != null) {
                try {
                    Files.deleteIfExists(tmpFile);
                } catch (Exception ignore) {
                    // ne masque pas l'erreur principale
                }
            }
        }
    }

    private String buildGroupKey(Map<String, String> row, List<String> cols) {
        return cols.stream()
                .map(c -> c + "=" + row.getOrDefault(c, ""))
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
    }
}
package com.mycompany.eai.camel.core.csv;

import org.apache.commons.csv.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;

@Component
public class CsvGroupByChunker {

    public void groupByColumnsChunked(
            InputStream input,
            List<String> groupByColumns,
            int chunkSize,
            Path tmpDir,
            BiConsumer<String, List<Map<String, String>>> onChunk
    ) {

        try {
            Files.createDirectories(tmpDir);
            Path tmpFile = Files.createTempFile(tmpDir, "csvgrp-", ".csv");

            // Pass 1 : écrire tout avec clé de groupe
            try (BufferedReader r = new BufferedReader(new InputStreamReader(input));
                 CSVParser p = new CSVParser(r, CSVFormat.DEFAULT.withFirstRecordAsHeader());
                 BufferedWriter w = Files.newBufferedWriter(tmpFile);
                 CSVPrinter out = new CSVPrinter(w, CSVFormat.DEFAULT)) {

                List<String> headers = new ArrayList<>(p.getHeaderMap().keySet());
                headers.add("__grpkey");
                out.printRecord(headers);

                for (CSVRecord rec : p) {
                    Map<String, String> row = rec.toMap();
                    String groupKey = buildGroupKey(row, groupByColumns);

                    List<String> values = new ArrayList<>(row.values());
                    values.add(groupKey);
                    out.printRecord(values);
                }
            }

            // Pass 2 : regroupement en mémoire (par groupe)
            Map<String, List<Map<String, String>>> groups = new LinkedHashMap<>();

            try (BufferedReader r = Files.newBufferedReader(tmpFile);
                 CSVParser p = new CSVParser(r, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

                for (CSVRecord rec : p) {
                    String groupKey = rec.get("__grpkey");
                    Map<String, String> row = rec.toMap();
                    row.remove("__grpkey");

                    groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(row);
                }
            }

            // Emit par chunks
            for (var e : groups.entrySet()) {
                List<Map<String, String>> rows = e.getValue();
                for (int i = 0; i < rows.size(); i += chunkSize) {
                    onChunk.accept(
                            e.getKey(),
                            rows.subList(i, Math.min(i + chunkSize, rows.size()))
                    );
                }
            }

            Files.deleteIfExists(tmpFile);

        } catch (Exception e) {
            throw new CsvGroupByException("CSV groupBy error", e);
        }
    }

    private String buildGroupKey(Map<String, String> row, List<String> cols) {
        return cols.stream()
                .map(c -> c + "=" + row.get(c))
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
    }
}
