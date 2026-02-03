// Dans votre méthode fromApp() de FixApplication
public void fromApp(Message message, SessionID sessionID) 
    throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
    
    crack(message, sessionID);
    
    if (message.getHeader().getString(MsgType.FIELD) == MsgType.TRADE_CAPTURE_REPORT) {
        // Logger et envoyer à la queue JMS (votre code actuel)
        logAndSendToQueue(message);
        
        // Envoyer l'acquittement
        sendTradeCaptureReportAck(message, sessionID);
    }
}

private void sendTradeCaptureReportAck(Message tradeReport, SessionID sessionID) 
    throws FieldNotFound {
    
    TradeCaptureReportAck ack = new TradeCaptureReportAck();
    
    // Tags obligatoires
    ack.set(new TradeReportID(tradeReport.getString(TradeReportID.FIELD))); // Tag 571
    ack.set(new TradeReportTransType(0)); // Tag 487: 0=New
    ack.set(new TradeReportType(0)); // Tag 856: 0=Submit
    ack.set(new TrdType(tradeReport.getChar(TrdType.FIELD))); // Tag 828
    ack.set(new ExecType(ExecType.TRADE)); // Tag 150: F=Trade
    ack.set(new TradeReportRefID(tradeReport.getString(TradeReportID.FIELD))); // Tag 572
    
    // Statut de l'acceptation
    ack.set(new TradeReportStatus(0)); // Tag 939: 0=Accepted
    
    // Copier les parties (si nécessaire selon spec TradeWeb)
    if (tradeReport.isSetField(TrdMatchID.FIELD)) {
        ack.set(new TrdMatchID(tradeReport.getString(TrdMatchID.FIELD))); // Tag 880
    }
    
    // Envoyer
    Session.sendToTarget(ack, sessionID);
    log.info("Trade Capture Report ACK sent for TradeReportID: {}", 
        tradeReport.getString(TradeReportID.FIELD));
}
