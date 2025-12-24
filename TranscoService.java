package com.mycompany.transco.service;

import com.mycompany.transco.model.Transcos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TranscoService {

    private final JdbcTemplate jdbcTemplate;

    // cache simple (tu peux remplacer par Caffeine/Ehcache)
    private final ConcurrentHashMap<String, Transcos> cache = new ConcurrentHashMap<>();

    private static final String SQL = """
        SELECT GS.CATEGORY,
               GS.APPLICATION AS "SRC",
               GS.VALUE       AS "SRC_VL",
               GT.APPLICATION AS "TGT",
               GT.VALUE       AS "TGT_VL"
        FROM MAESTRO.TBL_TRANS_GLOBAL_SOURCE GS,
             MAESTRO.TBL_TRANS_GLOBAL_TARGET GT
        WHERE GS.XCO_ID = GT.XCO_ID
          AND GS.CATEGORY = ?
        """;

    public TranscoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Transcos getTranscosByCategory(String category) {
        return cache.computeIfAbsent(category, this::loadFromDb);
    }

    private Transcos loadFromDb(String category) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, category);
        return TranscosMapper.fromRows(rows);
    }

    // optionnel: méthode pour invalider le cache
    public void evict(String category) { cache.remove(category); }
    public void clear() { cache.clear(); }
}
