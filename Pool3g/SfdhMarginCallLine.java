package fr.labanquepostale.report.base.model.pool3G;

import lombok.Data;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

import java.math.BigDecimal;
@Data
@CsvRecord(separator = "\\|", skipFirstLine = false, generateHeaderColumns = true)
public class SfdhMarginCallLine {

    @DataField(pos=1,columnName = "CONTRACT_ID")
    private String contractId;

    @DataField(pos=2,columnName = "CODE_BOM")
    private String codeBom;

    @DataField(pos=3,columnName = "DIRECTION")
    private String direction;

    @DataField(pos=4,columnName = "CURRENCY")
    private String currency;

    @DataField(pos=5,columnName = "NOMINAL")
    private String nominal;

    @DataField(pos=6,columnName = "CLEAN PRICE")
    private String cleanPrice;

    @DataField(pos=7,columnName = "ACCRUAL")
    private String accrual;

    @DataField(pos=8,columnName = "PV_CURRENCY")
    private String pvCurrency;

    @DataField(pos=9,columnName = "PV_EUR")
    private String pvEur;

    @DataField(pos=10,columnName = "PV_HC_CURRENCY")
    private String pvHcCurrency;

    @DataField(pos=11,columnName = "PV_HC_EUR")
    private String pvHcEur;

    @DataField(pos=12,columnName = "HAIRCUT")
    private String haircut;

    @DataField(pos=13,columnName = "DATE_TRT")
    private String dateTrt;

    @DataField(pos=14,columnName = "BANKRUPTCY_REMOTE")
    private String bankruptcyRemote;

    @DataField(pos=15,columnName = "NOMINAL_EURO")
    private String nominalEuro;

    public transient BigDecimal nominalValue = BigDecimal.ZERO;
    public transient BigDecimal nominalEuroValue = BigDecimal.ZERO;
}
