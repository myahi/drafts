package fr.labanquepostale.report.base.model.acadia;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

@Getter
@Setter
@CsvRecord(separator = "\t", skipFirstLine = true)
public class AcadiaIceLine {

    @DataField(pos = 1)
    private String valuationDate;

    @DataField(pos = 2)
    private String portfolioID;

    @DataField(pos = 3)
    private String tradeID;

    @DataField(pos = 4)
    private String endDate;

    @DataField(pos = 5)
    private String IMModel;

    @DataField(pos = 6)
    private String postRegulations;

    @DataField(pos = 7)
    private String collectRegulations;

    @DataField(pos = 8)
    private String productClass;

    @DataField(pos = 9)
    private String sensitivity_Id;

    @DataField(pos = 10)
    private String riskType;

    @DataField(pos = 11)
    private String qualifier;

    @DataField(pos = 12)
    private String bucket;

    @DataField(pos = 13)
    private String label1;

    @DataField(pos = 14)
    private String label2;

    @DataField(pos = 15)
    private String amount;

    @DataField(pos = 16)
    private String amountCurrency;

    @DataField(pos = 17)
    private String amountUSD;
}
