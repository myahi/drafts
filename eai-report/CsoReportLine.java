package fr.labanquepostale.report.base.model.acadia;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

@Getter
@Setter
@CsvRecord(separator = "\t", skipFirstLine = true)
public class CsoReportLine {

    @DataField(pos = 1)
    private String externalTradeID;

    @DataField(pos = 2)
    private String tradeID;
}
