@Service
public class MailingListService {

    private final GenericDbService db;

    public MailingListService(GenericDbService db) {
        this.db = db;
    }

    public MailingRecipients getRecipients(String mailingListName) {

        final String sql = """
            SELECT
                a.MAIL_ADDRESS     AS MAIL_ADDRESS,
                m.DESTINATION_TYPE AS DESTINATION_TYPE
            FROM TBL_MAIL_LISTS l
            JOIN TBL_MAIL_MAPPING m ON m.LIST_ID = l.LIST_ID
            JOIN TBL_MAIL_ADDRESSES a ON a.ADDRESS_ID = m.ADDRESS_ID
            WHERE l.LIST_NAME = :listName
        """;

        Map<String, Object> params = Map.of("listName", mailingListName);

        Set<String> to = new LinkedHashSet<>();
        Set<String> cc = new LinkedHashSet<>();

        for (Map<String, Object> row : db.select(sql, params)) {
            String email = (String) row.get("MAIL_ADDRESS");
            String type  = (String) row.get("DESTINATION_TYPE");

            if ("TO".equals(type)) {
                to.add(email);
            } else if ("CC".equals(type)) {
                cc.add(email);
            }
        }

        return new MailingRecipients(
                List.copyOf(to),
                List.copyOf(cc)
        );
    }
}
