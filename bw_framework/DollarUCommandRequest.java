package com.example.dollarU.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DollarUCommandRequest {

    private CommandParameters commandParameters;
    private List<Reference> references;   // optionnel
    private List<Metadata> metadatas;     // optionnel
    private String auditStatus;           // optionnel (si vide => on calculera)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandParameters {
        private String flowType;
        private int quantity;
        private String flowId;       // optionnel
        private String dollarUData;  // optionnel
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reference {
        private String code;
        private String codifier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String key;
        private String value;
    }
}
