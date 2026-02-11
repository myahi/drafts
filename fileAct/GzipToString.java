import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;

public final class GzipToString {

    private GzipToString() {}

    public static String gunzipToString(File gzFile, Charset charset) throws IOException {
        try (InputStream fis = new FileInputStream(gzFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GZIPInputStream gis = new GZIPInputStream(bis)) {

            return readAllToString(gis, charset);
        }
    }

    private static String readAllToString(InputStream in, Charset charset) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), charset);
    }
}
