@Data
@CsvRecord(separator=";")
public class SfdhMarginCallLine {

    @DataField(pos=1)
    private String contractId;

    @DataField(pos=2)
    private String codeBom;

    @DataField(pos=3)
    private String direction;

    @DataField(pos=4)
    private String currency;

    @DataField(pos=5)
    private String nominal;

    @DataField(pos=6)
    private String cleanPrice;

    @DataField(pos=7)
    private String accrual;

    @DataField(pos=8)
    private String pvCurrency;

    @DataField(pos=9)
    private String pvEur;

    @DataField(pos=10)
    private String pvHcCurrency;

    @DataField(pos=11)
    private String pvHcEur;

    @DataField(pos=12)
    private String haircut;

    @DataField(pos=13)
    private String dateTrt;

    @DataField(pos=14)
    private String bankruptcyRemote;

    @DataField(pos=15)
    private String nominalEuro;
}
