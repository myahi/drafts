import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ZipToString {

    private ZipToString() {}

    /**
     * Décompresse le premier fichier "normal" contenu dans le ZIP et retourne son contenu en String.
     * - Ne crée aucun dossier (tout en mémoire)
     * - Ignore les répertoires dans le ZIP
     */
    public static String unzipFirstFileToString(File zipFile, Charset charset) throws IOException {
        try (InputStream fis = new FileInputStream(zipFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                // On lit le contenu de cette entrée en mémoire
                return readAllToString(zis, charset);
            }
            throw new FileNotFoundException("ZIP vide ou ne contient aucun fichier.");
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
