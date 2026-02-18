private void sendTradeCaptureReportAck(Message message, SessionID sessionID) throws FieldNotFound, SessionNotFound {
		TradeCaptureReportAck ack = new TradeCaptureReportAck();
		ack.set(new TradeReportID(message.getString(TradeReportID.FIELD)));
		ack.set(new TradeReportTransType(0));
		Session.sendToTarget(ack, sessionID);
		LOGGER.info("Trade Capture Report ACK sent for TradeReportID: {}", message.getString(TradeReportID.FIELD));
	}
