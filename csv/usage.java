csvChunkReader.readChunksAsColumnMap(
    inputStream,
    CsvChunkReader.Options.defaults().hasHeader(true),
    1000,
    chunk -> {
        List<Trade> trades = chunk.stream()
            .map(row -> {
                Trade t = new Trade();
                t.setAccount(row.get("ACCOUNT"));
                t.setTradeDate(row.get("TRADE_DATE"));
                t.setAmount(new BigDecimal(row.get("AMOUNT")));
                return t;
            })
            .toList();

        processTrades(trades);
    }
);


csvGroupByChunker.groupByColumnsChunked(
    inputStream,
    List.of("ACCOUNT", "TRADE_DATE"),
    500,
    Path.of("/tmp"),
    (groupKey, chunk) -> {

        List<Trade> trades = chunk.stream()
            .map(row -> {
                Trade t = new Trade();
                t.setAccount(row.get("ACCOUNT"));
                t.setTradeDate(row.get("TRADE_DATE"));
                t.setAmount(new BigDecimal(row.get("AMOUNT")));
                return t;
            })
            .toList();

        processGroup(groupKey, trades);
    }
);
