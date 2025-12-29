package com.mycompany.transco.service;

import com.mycompany.transco.model.Transco;
import com.mycompany.transco.model.Transcos;
import com.mycompany.transco.model.Input;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TranscoResolver {

    /**
     * Retourne TOUTES les transcos qui matchent les conditions (AND strict).
     */
    public List<Transco> findAllTransco(Map<String, Transcos> transcosByCategory,
                                        String category,
                                        Map<String, String> conditions) {

        if (transcosByCategory == null || transcosByCategory.isEmpty()) {
            throw new IllegalArgumentException("transcosByCategory must not be null or empty");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be null or blank");
        }
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("conditions must not be null or empty");
        }

        Transcos group = transcosByCategory.get(category);
        if (group == null) {
            throw new IllegalArgumentException("Unknown transco category: " + category);
        }

        return group.getTransco().stream()
            .filter(t -> matches(t, conditions))
            .toList();
    }

    /**
     * Retourne UNE SEULE transco.
     * - 0 match  -> erreur
     * - >1 match -> erreur
     */
    public Transco findTransco(Map<String, Transcos> transcosByCategory,
                               String category,
                               Map<String, String> conditions) {

        List<Transco> matches = findAllTransco(transcosByCategory, category, conditions);

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                "No transco match for category=" + category + " conditions=" + conditions
            );
        }

        if (matches.size() > 1) {
            throw new IllegalStateException(
                "Multiple transcos match for category=" + category +
                " conditions=" + conditions +
                " (found=" + matches.size() + ")"
            );
        }

        return matches.get(0);
    }

    // ---------------------------------------------------------------------
    // Matching AND strict
    // ---------------------------------------------------------------------

    private boolean matches(Transco transco, Map<String, String> conditions) {

        Map<String, String> inputMap = transco.getInput().stream()
            .collect(Collectors.toMap(
                Input::getName,      // adapte si nécessaire
                Input::getValue,     // adapte si nécessaire
                (a, b) -> a          // en cas de doublon, on garde le premier
            ));

        // AND strict : toutes les conditions doivent matcher
        return conditions.entrySet().stream()
            .allMatch(e -> Objects.equals(inputMap.get(e.getKey()), e.getValue()));
    }
}
