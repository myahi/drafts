package fr.labanquepostale.report.base.beans.pool3G;

import fr.labanquepostale.report.base.model.pool3G.CalypsoMarginCall;
import fr.labanquepostale.report.base.model.pool3G.SfdhMarginCallLine;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class MarginCallProcessor implements AggregationStrategy {

    // ---- map CSV -> SFDH + copie des typed values ----
    public SfdhMarginCallLine map(CalypsoMarginCall input) {
        SfdhMarginCallLine output = new SfdhMarginCallLine();

        output.setContractId(input.CONTRACT_ID);
        output.setCodeBom(input.CODE_BOM);
        output.setDirection(input.DIRECTION);
        output.setCurrency(input.CURRENCY);

        // strings nettoyées (comme BW)
        output.setNominal(commaToDot(remove(input.NOMINAL)));
        output.setCleanPrice(remove(input.CLEAN_PRICE));

        String acc = remove(input.ACCRUAL);
        output.setAccrual((acc != null && !acc.isBlank()) ? acc : "0,0");

        output.setPvCurrency(remove(input.PV_CURRENCY));
        output.setPvEur(remove(input.PV_EUR));
        output.setPvHcCurrency(remove(input.PV_HC_CURRENCY));
        output.setPvHcEur(remove(input.PV_HC_EUR));
        output.setHaircut(remove(input.HAIRCUT));

        output.setDateTrt(input.DATE_TRT);
        output.setBankruptcyRemote(remove(input.BANKRUPTCY_REMOTE));

        output.setNominalEuro(commaToDot(remove(input.NOMINAL_EURO)));

        output.setNominalValue((input.nominalValue != null) ? input.nominalValue : BigDecimal.ZERO);
        output.setNominalEuroValue((input.nominalEuroValue != null) ? input.nominalEuroValue : BigDecimal.ZERO);

        return output;
    }

    // ---- aggregate : somme directement sur BigDecimal internes ----
    @Override
    public Exchange aggregate(Exchange oldEx, Exchange newEx) {
        SfdhMarginCallLine row = newEx.getIn().getBody(SfdhMarginCallLine.class);

        BigDecimal n = row.nominalValue != null ? row.nominalValue : BigDecimal.ZERO;
        BigDecimal e = row.nominalEuroValue != null ? row.nominalEuroValue : BigDecimal.ZERO;

        if (oldEx == null) {
            // 1ère ligne du groupe = conservée (comme current-group()[1])
            // on la garde et elle porte déjà ses valeurs typées
            return newEx;
        }

        SfdhMarginCallLine kept = oldEx.getIn().getBody(SfdhMarginCallLine.class);
        kept.nominalValue = safe(kept.nominalValue).add(n);
        kept.nominalEuroValue = safe(kept.nominalEuroValue).add(e);

        // on ne touche pas aux autres champs => ceux de la 1ère ligne restent
        return oldEx;
    }

    // ---- finalize : écrit les sommes formatées (virgule) dans les champs CSV ----
    public void finalizeGroup(Exchange exchange) {
        SfdhMarginCallLine kept = exchange.getIn().getBody(SfdhMarginCallLine.class);
        kept.setNominal(formatWithComma(kept.nominalValue));
        kept.setNominalEuro(formatWithComma(kept.nominalEuroValue));
    }

    // ---- helpers ----
    private BigDecimal safe(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private String remove(String s) {
        if (s == null) return null;
        return s.replaceAll("[^0-9A-Za-z,\\.\\-]", "");
    }

    private String commaToDot(String s) {
        return s == null ? null : s.replace(',', '.');
    }

    private String formatWithComma(BigDecimal v) {
        if (v == null) v = BigDecimal.ZERO;
        return v.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
