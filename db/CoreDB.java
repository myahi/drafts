package com.mycompany.eai.camel.core.db;

public class DbAccessException extends RuntimeException {

    public DbAccessException(String message) {
        super(message);
    }

    public DbAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}


package com.mycompany.eai.camel.core.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Component
public class GenericDbService {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcTemplate jdbcTemplate;

    public GenericDbService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbc = new NamedParameterJdbcTemplate(this.jdbcTemplate);
    }

    // INSERT / UPDATE / DELETE
    @Transactional
    public int update(String sql, Map<String, ?> params) {
        try {
            return jdbc.update(sql, params);
        } catch (Exception e) {
            throw new DbAccessException("Error executing update SQL", e);
        }
    }

    // SELECT 1 ligne
    @Transactional(readOnly = true)
    public Map<String, Object> selectOne(String sql, Map<String, ?> params) {
        try {
            return jdbc.queryForMap(sql, params);
        } catch (Exception e) {
            throw new DbAccessException("Error executing selectOne SQL", e);
        }
    }

    // SELECT N lignes
    @Transactional(readOnly = true)
    public List<Map<String, Object>> select(String sql, Map<String, ?> params) {
        try {
            return jdbc.queryForList(sql, params);
        } catch (Exception e) {
            throw new DbAccessException("Error executing select SQL", e);
        }
    }

    // Batch INSERT / UPDATE / DELETE
    @Transactional
    public int[] batchUpdate(String sql, List<Map<String, ?>> batchParams) {
        try {
            return jdbc.batchUpdate(sql, batchParams.toArray(new Map[0]));
        } catch (Exception e) {
            throw new DbAccessException("Error executing batch update SQL", e);
        }
    }

    // Appel de procédure stockée Oracle
    @Transactional
    public Map<String, Object> callProcedure(
            String catalog,
            String procedure,
            Map<String, ?> inParams) {

        try {
            SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName(procedure);

            if (catalog != null && !catalog.isBlank()) {
                call = call.withCatalogName(catalog);
            }

            return call.execute(inParams);

        } catch (Exception e) {
            throw new DbAccessException(
                    "Error calling procedure " +
                    (catalog != null ? catalog + "." : "") + procedure, e);
        }
    }

    // Appel de fonction Oracle
    @Transactional(readOnly = true)
    public Object callFunction(
            String catalog,
            String function,
            Map<String, ?> inParams) {

        try {
            SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                    .withFunctionName(function);

            if (catalog != null && !catalog.isBlank()) {
                call = call.withCatalogName(catalog);
            }

            return call.executeFunction(Object.class, inParams);

        } catch (Exception e) {
            throw new DbAccessException(
                    "Error calling function " +
                    (catalog != null ? catalog + "." : "") + function, e);
        }
    }
}
