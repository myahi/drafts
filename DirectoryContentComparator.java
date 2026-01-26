import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

public class DirectoryContentComparator {

    public static void main(String[] args) throws Exception {
        Path dirA = Paths.get("dirA");
        Path dirB = Paths.get("dirB");

        Set<String> hashesB = computeDirectoryHashes(dirB);

        for (Path fileA : Files.walk(dirA).filter(Files::isRegularFile).toList()) {
            String hashA = hashFile(fileA);
            if (!hashesB.contains(hashA)) {
                System.out.println("Fichier manquant (contenu unique) : " + fileA);
            }
        }
    }

    private static Set<String> computeDirectoryHashes(Path dir) throws Exception {
        Set<String> hashes = new HashSet<>();
        for (Path file : Files.walk(dir).filter(Files::isRegularFile).toList()) {
            hashes.add(hashFile(file));
        }
        return hashes;
    }

    private static String hashFile(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        return bytesToHex(digest.digest());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
