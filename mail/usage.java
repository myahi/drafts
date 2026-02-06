from("direct:mailingList")
  .bean(MailingListService.class, "getRecipients(${header.listName})")
  .process(e -> {
      MailingRecipients r = e.getMessage().getBody(MailingRecipients.class);
      e.getMessage().setHeader("to", String.join(",", r.to()));
      e.getMessage().setHeader("cc", String.join(",", r.cc()));
  });
