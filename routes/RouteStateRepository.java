package fr.labanquepostale.marches.eai.core.route.lifecycle;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RouteStateRepository {

	private final JdbcTemplate jdbc;

	public RouteStateRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void upsert(String instanceName, String routeId, String desiredState) {
		jdbc.update("""
				merge into CAMEL_ROUTE_STATE t
				using (
				  select ? as INSTANCE_NAME,
				         ? as ROUTE_ID,
				         ? as DESIRED_STATE,
				         ? as UPDATED_AT
				  from dual
				) s
				on (
				  t.INSTANCE_NAME = s.INSTANCE_NAME
				  and t.ROUTE_ID = s.ROUTE_ID
				)
				when matched then
				  update set
				    t.DESIRED_STATE = s.DESIRED_STATE,
				    t.UPDATED_AT = s.UPDATED_AT
				when not matched then
				  insert (INSTANCE_NAME, ROUTE_ID, DESIRED_STATE, UPDATED_AT)
				  values (s.INSTANCE_NAME, s.ROUTE_ID, s.DESIRED_STATE, s.UPDATED_AT)
				""", instanceName, routeId, desiredState, Timestamp.from(Instant.now()));
	}

	public Map<String, String> loadAllForInstance(String instanceName) {
		return jdbc.query("""
				select ROUTE_ID, DESIRED_STATE
				from CAMEL_ROUTE_STATE
				where INSTANCE_NAME = ?
				""", rs -> {
			Map<String, String> map = new HashMap<>();
			while (rs.next()) {
				map.put(rs.getString("ROUTE_ID"), rs.getString("DESIRED_STATE"));
			}
			return map;
		}, instanceName);
	}
}
