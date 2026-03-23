package fr.labanquepostale.report.base.beans.acadia;

import fr.labanquepostale.report.base.model.acadia.AcadiaIceLine;
import fr.labanquepostale.report.base.model.acadia.CsoReportLine;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PrepareFileForAcadiaBean {

    public Map<String, String> buildTradeIdMapping(@SuppressWarnings("unchecked") List<CsoReportLine> lines) {
        Map<String, String> result = new HashMap<>();

        for (CsoReportLine line : lines) {
            if (isNotBlank(line.getTradeID()) && isNotBlank(line.getExternalTradeID())) {
                result.put(line.getTradeID().trim(), line.getExternalTradeID().trim());
            }
        }

        return result;
    }

    public String readFile(String fullPath, String encoding) throws IOException {
        return Files.readString(Paths.get(fullPath), Charset.forName(encoding));
    }

    /**
     * Reproduit la logique TIBCO :
     * PortfolioID = nom du fichier sans extension avant le dernier "_"
     * Exemple : PFOLIO_20260323.txt -> PFOLIO
     */
    public String extractPortfolioIdFromFileName(String inputFileName) {
        String fileNameWithoutExtension = removeExtension(inputFileName);
        int idx = fileNameWithoutExtension.lastIndexOf('_');
        return idx > 0 ? fileNameWithoutExtension.substring(0, idx) : fileNameWithoutExtension;
    }

    public String buildOutputFileName(String inputFileName, String outputExtension) {
        return removeExtension(inputFileName) + outputExtension;
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

    private String toTabLine(AcadiaIceLine line) {
        return String.join("\t",
                nvl(line.getValuationDate()),
                nvl(line.getPortfolioID()),
                nvl(line.getTradeID()),
                nvl(line.getEndDate()),
                nvl(line.getIMModel()),
                nvl(line.getPostRegulations()),
                nvl(line.getCollectRegulations()),
                nvl(line.getProductClass()),
                nvl(line.getSensitivity_Id()),
                nvl(line.getRiskType()),
                nvl(line.getQualifier()),
                nvl(line.getBucket()),
                nvl(line.getLabel1()),
                nvl(line.getLabel2()),
                nvl(line.getAmount()),
                nvl(line.getAmountCurrency()),
                nvl(line.getAmountUSD())
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
