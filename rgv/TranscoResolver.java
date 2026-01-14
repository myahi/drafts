package fr.lbp.lib.service;



import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import fr.lbp.lib.model.transco.Transco;
import fr.lbp.lib.model.transco.Transcos;

@Component
public class TranscoResolver {

    
	public List<Transco> findAllTransco(Map<String, Transcos> transcosByCategory, String category, Map<String, String> srcConditions) {

		if (transcosByCategory == null || transcosByCategory.isEmpty()) {
			throw new IllegalArgumentException("transcosByCategory must not be null or empty");
		}
		if (category == null || category.isBlank()) {
			throw new IllegalArgumentException("category must not be null or blank");
		}
		if (srcConditions == null || srcConditions.isEmpty()) {
			throw new IllegalArgumentException("conditions must not be null or empty");
		}

		Transcos group = transcosByCategory.get(category);
		if (group == null) {
			throw new IllegalArgumentException("Unknown transco category: " + category);
		}

		return group.getTransco().stream().filter(t -> matches(t, srcConditions)).toList();
	}

	public Transco findTransco(Map<String, Transcos> transcosByCategory, String category, Map<String, String> conditions) {

		List<Transco> matches = findAllTransco(transcosByCategory, category, conditions);

		return matches != null && matches.size() > 0 ? matches.get(0) : null;
	}

	private boolean matches(Transco transco, Map<String, String> srcConditions) {
		boolean found = false;
		for (Map.Entry<String, String> entry : srcConditions.entrySet()) {
			String key = entry.getKey();
			switch (key) {
			case "category": {
				if (entry.getValue().equals(transco.getCategory())) {
					found = true;
					break;
				} else {
					return false;
				}
			}
			case "srcApp": {
				if (entry.getValue().equals(transco.getSrcApp())) {
					found = true;
					break;
				} else {
					return false;
				}
			}
			case "srcVl": {
				if (entry.getValue().equals(transco.getSrcVl())) {
					found = true;
					break;
				} else {
					return false;
				}
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + key);
			}

		}
		return found;
	}
}
