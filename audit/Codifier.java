package fr.labanquepostale.marches.eai.core.model.audit;

public enum Codifier {

  ABI_VERSION("ABINumSeq"),
  AMOUNT("AMOUNT"),
  DATE("DATEID"),
  DCL_ID("DCLID"),
  DCO_DATE("DATERECEPID"),
  DCO_ID("DCOID"),
  DECLARATION_ID("DECLARATIONID"),
  EXTERNAL_ID("EXT_ID"),
  FILE_ID("FILEID"),
  FILE_NAME("FILE_NAME"),
  FLOW_ID("FLOW_ID"),
  MAIL_ID("MAILID"),
  MARKIT_ID("MARKITID"),
  MESSAGE_ID("MESSAGEID"),
  RDT_ID("RDTID"),
  RGV_ID("RGVID"),
  TECHNICAL_ID("TECHNICAL_ID"),
  TRADE_ID("TRADEID"),
  TRANSFERT_ID("TRANSFERTID");

  private final String code;

  Codifier(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
