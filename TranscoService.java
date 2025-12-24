package com.mycompany.transco.service;

import com.mycompany.transco.model.Transcos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'accès Oracle pour charger les transcos (TBL_TRANS_GLOBAL_SOURCE/TBL_TRANS_GLOBAL_TARGET)
 * et les mapper vers l'objet JAXB {@link Transcos}.
 *
 * Version SANS cache (comme tu l'as demandé).
 *
 * - loadByCategory(...) : 1 catégorie -> 1 requête
 * - loadByCategories(...) : N catégories -> 1 requête avec IN (...)
 *
 * Notes Oracle:
 * - Attention aux listes IN trop longues. Si tu risques d'avoir > 900/1000 catégories,
 *   utilise loadByCategoriesBatched(...).
 */
@Service
public class TranscoService {

    private final JdbcTemplate jdbcTemplate;

    // Base SQL (on injecte dynamiquement la clause IN si besoin)
    private static final String SQL_BASE = """
        SELECT GS.CATEGORY,
               GS.APPLICATION AS "SRC",
               GS.VALUE       AS "SRC_VL",
               GT.APPLICATION AS "TGT",
               GT.VALUE       AS "TGT_VL"
        FROM MAESTRO.TBL_TRANS_GLOBAL_SOURCE GS
        JOIN MAESTRO.TBL_TRANS_GLOBAL_TARGET GT
          ON GS.XCO_ID = GT.XCO_ID
        """;

    // SQL pour une catégorie
    private static final String SQL_ONE = SQL_BASE + """
        WHERE GS.CATEGORY = ?
        """;

    public TranscoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Charge les transcos pour une seule catégorie.
     */
    public Transcos loadByCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be null/blank");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL_ONE, category);
        return TranscosMapper.fromRows(rows);
    }

    /**
     * Charge les transcos pour une liste de catégories en une seule requête SQL.
     *
     * @return Map category -> Transcos
     */
    public Map<String, Transcos> loadByCategories(List<String> categories) {
        List<String> cats = normalize(categories);
        if (cats.isEmpty()) {
            return Map.of();
        }

        String placeholders = cats.stream().map(c -> "?").collect(Collectors.joining(","));
        String sql = SQL_BASE + " WHERE GS.CATEGORY IN (" + placeholders + ")";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, cats.toArray());
        return TranscosMapper.fromRowsGroupedByCategory(rows);
    }

    /**
     * Variante robuste si tu peux avoir de grosses listes (ex: > 1000 catégories).
     * Oracle a une limite pratique sur la taille des IN-list selon contexte (et ça devient illisible).
     *
     * Cette méthode split en batch et agrège le résultat.
     */
    public Map<String, Transcos> loadByCategoriesBatched(List<String> categories, int batchSize) {
        List<String> cats = normalize(categories);
        if (cats.isEmpty()) {
            return Map.of();
        }
        if (batchSize <= 0) {
            batchSize = 500;
        }

        Map<String, Transcos> aggregated = new LinkedHashMap<>();

        for (int i = 0; i < cats.size(); i += batchSize) {
            List<String> batch = cats.subList(i, Math.min(i + batchSize, cats.size()));
            Map<String, Transcos> partial = loadByCategories(batch);

            // Merge: si une catégorie existe déjà (batchs multiples), on concatène les <transco>
            for (Map.Entry<String, Transcos> entry : partial.entrySet()) {
                aggregated.merge(entry.getKey(), entry.getValue(), (a, b) -> {
                    // a et b ont même category; on ajoute les transco de b dans a
                    a.getTransco().addAll(b.getTransco());
                    return a;
                });
            }
        }
        return aggregated;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static List<String> normalize(List<String> categories) {
        if (categories == null) return List.of();

        // - trim
        // - enlève null/blank
        // - dédoublonne (LinkedHashSet pour préserver l'ordre)
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String c : categories) {
            if (c == null) continue;
            String v = c.trim();
            if (!v.isEmpty()) {
                set.add(v);
            }
        }
        return new ArrayList<>(set);
    }
}
