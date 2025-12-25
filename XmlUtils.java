import jakarta.xml.bind.*;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;

public class XmlUtils {

    private static final ConcurrentHashMap<Class<?>, JAXBContext> CACHE = new ConcurrentHashMap<>();

    private static JAXBContext getContext(Class<?> type) throws JAXBException {
        return CACHE.computeIfAbsent(type, t -> {
            try {
                return JAXBContext.newInstance(t);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <T> String marshallToString(T object) throws Exception {

        JAXBContext context = getContext(object.getClass());
        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        try (StringWriter writer = new StringWriter()) {
            marshaller.marshal(object, writer);
            return writer.toString();
        }
    }
}
