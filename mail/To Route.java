from("direct:notify")
  .process(e -> {
    e.getMessage().setHeader("listName", "TRADEWEB_ALERTS");
    e.getMessage().setHeader("subject", "Rapport");

    // content-type du body
    e.getMessage().setHeader("Content-Type", "text/plain; charset=UTF-8");
    e.getMessage().setBody("Bonjour, rapport en pièce jointe.");

    // pièce jointe
    var am = e.getMessage(org.apache.camel.attachment.AttachmentMessage.class);
    var file = new java.io.File("/tmp/report.csv");
    var ds = new jakarta.activation.FileDataSource(file);
    am.addAttachment("report.csv", new jakarta.activation.DataHandler(ds));
  })
  .to("direct:core.sendMailByList");
