logging.level.org.apache.camel=INFO
logging.level.org.apache.camel.impl=INFO
logging.level.org.apache.camel.support=WARN


public static List<Path> listFiles(
        Exchange exchange,
        Path dir,
        boolean recursive,
        SortCriterion sort,
        SortDirection direction,
        String filenameRegex
) throws IOException {

    try (Stream<Path> s = recursive ? Files.walk(dir) : Files.list(dir)) {

        Comparator<Path> cmp = comparator(sort);
        if (direction == SortDirection.DESC) {
            cmp = cmp.reversed();
        }

        Pattern pattern = filenameRegex != null ? Pattern.compile(filenameRegex) : null;

        List<Path> files = s
            .filter(Files::isRegularFile)
            .filter(p -> pattern == null || pattern.matcher(p.getFileName().toString()).matches())
            .sorted(cmp)
            .collect(Collectors.toList());

        // ✅ Stockage dans l'exchange Camel
        if (exchange != null) {
            exchange.setProperty("inputFiles", files);

            List<String> filenames = files.stream()
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());

            exchange.setProperty("inputFileNames", filenames);
        }

        return files;
    }
}
