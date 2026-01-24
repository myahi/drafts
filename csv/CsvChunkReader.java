package com.mycompany.eai.camel.core.csv;

import org.apache.commons.csv.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;

@Component
public class CsvChunkReader {

    public static class Options {
        private Charset charset = Charset.forName("UTF-8");
        private char delimiter = ';';
        private boolean hasHeader = true;

        public Options charset(Charset c) { this.charset = c; return this; }
        public Options delimiter(char d) { this.delimiter = d; return this; }
        public Options hasHeader(boolean h) { this.hasHeader = h; return this; }

        public Charset getCharset() { return charset; }
        public char getDelimiter() { return delimiter; }
        public boolean hasHeader() { return hasHeader; }

        public static Options defaults() { return new Options(); }
    }

    public void readChunksAsColumnMap(
            InputStream input,
            Options options,
            int chunkSize,
            Consumer<List<Map<String, String>>> onChunk
    ) {

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(input, options.getCharset()));
             CSVParser parser =
                     new CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(options.getDelimiter()))) {

            Iterator<CSVRecord> it = parser.iterator();
            List<String> headers = null;

            if (options.hasHeader() && it.hasNext()) {
                CSVRecord h = it.next();
                headers = new ArrayList<>();
                h.forEach(headers::add);
            }

            List<Map<String, String>> chunk = new ArrayList<>(chunkSize);

            while (it.hasNext()) {
                CSVRecord r = it.next();

                if (headers == null) {
                    headers = new ArrayList<>();
                    for (int i = 0; i < r.size(); i++) headers.add("col" + i);
                }

                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i), i < r.size() ? r.get(i) : null);
                }

                chunk.add(row);

                if (chunk.size() == chunkSize) {
                    onChunk.accept(chunk);
                    chunk = new ArrayList<>(chunkSize);
                }
            }

            if (!chunk.isEmpty()) {
                onChunk.accept(chunk);
            }

        } catch (Exception e) {
            throw new CsvParseException("CSV parsing error", e);
        }
    }
}
