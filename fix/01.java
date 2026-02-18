private void sendAllocationInstructionAck(Message message,
                                          SessionID sessionID)
        throws FieldNotFound, SessionNotFound {

    AllocationInstructionAck ack =
            new AllocationInstructionAck();

    // === Champs obligatoires ===

    // 70 = AllocID (doit être repris tel quel du AS)
    ack.set(new AllocID(
            message.getString(AllocID.FIELD)));

    // 87 = AllocStatus (repris du AS)
    ack.set(new AllocStatus(
            message.getInt(AllocStatus.FIELD)));

    // 796 = AllocAckStatus
    ack.set(new AllocAckStatus(
            AllocAckStatus.ACCEPTED));

    // 60 = TransactTime (souvent requis par le sell-side)
    ack.set(new TransactTime(
            LocalDateTime.now()));

    // === Envoi ===
    Session.sendToTarget(ack, sessionID);

    LOGGER.info("AllocationInstructionAck sent for AllocID: {}",
            message.getString(AllocID.FIELD));
}



private void sendTradeCaptureReportAck(Message message, SessionID sessionID) throws FieldNotFound, SessionNotFound {
		TradeCaptureReportAck ack = new TradeCaptureReportAck();
		ack.set(new TradeReportID(message.getString(TradeReportID.FIELD)));
		ack.set(new TradeReportTransType(0));
		Session.sendToTarget(ack, sessionID);
		LOGGER.info("Trade Capture Report ACK sent for TradeReportID: {}", message.getString(TradeReportID.FIELD));
	}
