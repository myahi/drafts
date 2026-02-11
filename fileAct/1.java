import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public final class GzipToString {

    private GzipToString() {}

    public static String gunzipToString(File gzFile, Charset charset) throws IOException {
        try (InputStream fis = new FileInputStream(gzFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GZIPInputStream gis = new GZIPInputStream(bis);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int n;
            while ((n = gis.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), charset);
        }
    }

    public static void main(String[] args) throws Exception {
        File gz = new File(args[0]);
        String text = gunzipToString(gz, StandardCharsets.UTF_8); // ou ISO_8859_1
        System.out.println(text.substring(0, Math.min(2000, text.length())));
    }
}
