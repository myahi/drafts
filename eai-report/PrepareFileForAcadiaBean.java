package fr.labanquepostale.report.base.beans.acadia;

import fr.labanquepostale.report.base.model.acadia.AcadiaIceLine;
import fr.labanquepostale.report.base.model.acadia.CsoReportLine;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PrepareFileForAcadiaBean {

    public Map<String, String> buildTradeIdMapping(@SuppressWarnings("unchecked") List<CsoReportLine> lines) {
        Map<String, String> result = new HashMap<>();

        for (CsoReportLine line : lines) {
            if (line.getTradeID() != null && !line.getTradeID().isBlank()
                    && line.getExternalTradeID() != null && !line.getExternalTradeID().isBlank()) {
                result.put(line.getTradeID().trim(), line.getExternalTradeID().trim());
            }
        }

        return result;
    }

    public List<String> listIceFiles(String directory, String regexPattern, String csoFileName) throws IOException {
        Path dir = Paths.get(directory);
        List<String> result = new ArrayList<>();

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return result;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                String fileName = path.getFileName().toString();

                if (csoFileName != null && csoFileName.equals(fileName)) {
                    continue;
                }

                if (regexPattern != null && !regexPattern.isBlank() && !fileName.matches(regexPattern)) {
                    continue;
                }

                result.add(path.toAbsolutePath().toString());
            }
        }

        result.sort(Comparator.reverseOrder());
        return result;
    }

    public String readFile(String fullPath, String encoding) throws IOException {
        return Files.readString(Paths.get(fullPath), Charset.forName(encoding));
    }

    public String extractFileName(String fullPath) {
        return Paths.get(fullPath).getFileName().toString();
    }

    /**
     * Reproduit la logique TIBCO :
     * PortfolioID = nom du fichier avant le dernier "_"
     * Exemple : PFOLIO_20260323.txt -> PFOLIO
     */
    public String extractPortfolioIdFromFileName(String inputFileName) {
        String fileNameWithoutExtension = removeExtension(inputFileName);
        int idx = fileNameWithoutExtension.lastIndexOf('_');
        return idx > 0 ? fileNameWithoutExtension.substring(0, idx) : fileNameWithoutExtension;
    }

    public String buildOutputFileName(String inputFileName, String extension) {
        String baseName = removeExtension(inputFileName);
        return baseName + extension;
    }

    public List<AcadiaIceLine> transformIceLines(List<AcadiaIceLine> lines,
                                                 Map<String, String> tradeIdMapping,
                                                 String portfolioId) {
        List<AcadiaIceLine> result = new ArrayList<>();

        for (AcadiaIceLine line : lines) {
            AcadiaIceLine out = new AcadiaIceLine();

            out.setValuationDate(trim(line.getValuationDate()));
            out.setPortfolioID(portfolioId);

            String originalTradeId = trim(line.getTradeID());
            String lookupTradeId = removeLast3Chars(originalTradeId);
            String externalTradeId = tradeIdMapping.get(lookupTradeId);

            out.setTradeID(isNotBlank(externalTradeId) ? externalTradeId : originalTradeId);
            out.setEndDate(trim(line.getEndDate()));
            out.setIMModel(trim(line.getIMModel()));
            out.setPostRegulations(trim(line.getPostRegulations()));
            out.setCollectRegulations(trim(line.getCollectRegulations()));
            out.setProductClass(trim(line.getProductClass()));
            out.setSensitivity_Id(trim(line.getSensitivity_Id()));
            out.setRiskType(trim(line.getRiskType()));
            out.setQualifier(trim(line.getQualifier()));
            out.setBucket(trim(line.getBucket()));
            out.setLabel1(trim(line.getLabel1()));
            out.setLabel2(trim(line.getLabel2()));
            out.setAmount(trim(line.getAmount()));
            out.setAmountCurrency(trim(line.getAmountCurrency()));
            out.setAmountUSD(trim(line.getAmountUSD()));

            result.add(out);
        }

        return result;
    }

    public String toOutputBody(List<AcadiaIceLine> lines) {
        return lines.stream()
                .map(this::toTabLine)
                .collect(Collectors.joining("\n"));
    }

    private String toTabLine(AcadiaIceLine l) {
        return String.join("\t",
                nvl(l.getValuationDate()),
                nvl(l.getPortfolioID()),
                nvl(l.getTradeID()),
                nvl(l.getEndDate()),
                nvl(l.getIMModel()),
                nvl(l.getPostRegulations()),
                nvl(l.getCollectRegulations()),
                nvl(l.getProductClass()),
                nvl(l.getSensitivity_Id()),
                nvl(l.getRiskType()),
                nvl(l.getQualifier()),
                nvl(l.getBucket()),
                nvl(l.getLabel1()),
                nvl(l.getLabel2()),
                nvl(l.getAmount()),
                nvl(l.getAmountCurrency()),
                nvl(l.getAmountUSD())
        );
    }

    private String removeExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    private String removeLast3Chars(String value) {
        if (value == null || value.length() <= 3) {
            return value;
        }
        return value.substring(0, value.length() - 3);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
