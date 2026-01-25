List<FixedLengthFormatter.FieldDef> layout = List.of(
    new FixedLengthFormatter.FieldDef("ACCOUNT", 10).align(FixedLengthFormatter.Align.LEFT).paddingChar(' '),
    new FixedLengthFormatter.FieldDef("TRADE_DATE", 8).pattern("yyyyMMdd").clip(true),
    new FixedLengthFormatter.FieldDef("AMOUNT", 12)
        .align(FixedLengthFormatter.Align.RIGHT)
        .paddingChar('0')
        .precision(2)
        .impliedDecimal(true) // 123.45 -> 12345
);

Map<String, Object> row = Map.of(
    "ACCOUNT", "A1",
    "TRADE_DATE", java.time.LocalDate.of(2026, 1, 25),
    "AMOUNT", new java.math.BigDecimal("123.45")
);

String line = fixedLengthFormatter.formatLine(layout, row, FixedLengthFormatter.Options.defaults());
// -> "A1        20260125000000012345" (selon tes longueurs/options)


List<FixedLengthFormatter.FieldDef> layout = List.of(

    // 👉 hérite du padding global (LEFT + ' ')
    new FixedLengthFormatter.FieldDef("ACCOUNT", 10),

    // 👉 override alignement seulement
    new FixedLengthFormatter.FieldDef("TYPE", 2)
        .align(FixedLengthFormatter.Align.RIGHT),

    // 👉 override alignement + padding (numérique)
    new FixedLengthFormatter.FieldDef("AMOUNT", 12)
        .align(FixedLengthFormatter.Align.RIGHT)
        .paddingChar('0')
        .precision(2)
        .impliedDecimal(true),

    // 👉 override padding uniquement
    new FixedLengthFormatter.FieldDef("CODE", 5)
        .paddingChar('_')
);
