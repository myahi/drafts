@Service
public class TranscoService {

  private final JdbcTemplate jdbcTemplate;

  public TranscoService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, Transcos> loadByCategories(List<String> categories) {

    if (categories == null || categories.isEmpty()) {
      return Map.of();
    }

    String inSql = categories.stream()
        .map(c -> "?")
        .collect(Collectors.joining(","));

    String sql = """
      SELECT GS.CATEGORY,
             GS.APPLICATION AS "SRC",
             GS.VALUE       AS "SRC_VL",
             GT.APPLICATION AS "TGT",
             GT.VALUE       AS "TGT_VL"
      FROM MAESTRO.TBL_TRANS_GLOBAL_SOURCE GS
      JOIN MAESTRO.TBL_TRANS_GLOBAL_TARGET GT
        ON GS.XCO_ID = GT.XCO_ID
      WHERE GS.CATEGORY IN (%s)
    """.formatted(inSql);

    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(sql, categories.toArray());

    return TranscosMapper.fromRowsGroupedByCategory(rows);
  }
}
