public static Map<String, Transcos> fromRowsGroupedByCategory(
        List<Map<String, Object>> rows) {

  Map<String, Transcos> result = new HashMap<>();

  for (Map<String, Object> r : rows) {
    String category = String.valueOf(r.get("CATEGORY"));
    String src = String.valueOf(r.get("SRC"));
    String tgt = String.valueOf(r.get("TGT"));

    Transcos transcos = result.computeIfAbsent(
        category,
        c -> new Transcos(src, tgt)
    );

    Transco t = new Transco(category);
    t.getInput().add(new Input(src, str(r.get("SRC_VL"))));
    t.getOutput().add(new Output(tgt, str(r.get("TGT_VL"))));

    transcos.getTransco().add(t);
  }

  return result;
}
