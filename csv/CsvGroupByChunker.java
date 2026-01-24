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
