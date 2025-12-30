@Component
public class RgvMtResponseBean {

    public RGVMT buildResponse(Exchange exchange) {
        String fixed = exchange.getMessage().getBody(String.class);

        RGVMT resp = new RGVMT();
        resp.setMTformat("MT065");          // ou depuis une property
        resp.setRGVstring(fixed);
        // resp.setRGVWarning("...") si besoin

        // important: retourner l'objet
        return resp;
    }
}
