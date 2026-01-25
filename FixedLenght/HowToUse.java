//with layout 
List<FixedLengthChunkReader.FieldDef> layout = List.of(
    new FixedLengthChunkReader.FieldDef("ACCOUNT", 10),
    new FixedLengthChunkReader.FieldDef("TRADE_DATE", 8),
    new FixedLengthChunkReader.FieldDef("AMOUNT", 12)
);
fixedLengthChunkReader.readChunksAsFieldMap(
    inputStream,
    FixedLengthChunkReader.Options.defaults(),
    layout,
    500,
    chunk -> { /* métier */ }
); 

//without layout

int[] lengths = {10, 8, 12};

fixedLengthChunkReader.readChunksAsIndexMap(
    inputStream,
    FixedLengthChunkReader.Options.defaults(),
    lengths,
    500,
    chunk -> { /* métier */ }
);

