package fr.labanquepostale.marches.eai.core.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.camel.Exchange;

public final class FileMoveUtil {

  private FileMoveUtil() {}

  /**
   * Copy file(s) referenced by an Exchange property into a target directory.
   *
   * @param exchange         Camel exchange
   * @param targetDirOrProp  either a literal directory path or the name of an exchange property containing it
   * @param filesProp        name of exchange property containing the file(s) to copy (Path/String/List)
   */
  public static void copy(Exchange exchange, String targetDirOrProp, String filesProp) throws Exception {
    transfer(exchange, targetDirOrProp, filesProp, Operation.COPY);
  }

  /**
   * Move file(s) referenced by an Exchange property into a target directory.
   *
   * @param exchange         Camel exchange
   * @param targetDirOrProp  either a literal directory path or the name of an exchange property containing it
   * @param filesProp        name of exchange property containing the file(s) to move (Path/String/List)
   */
  public static void move(Exchange exchange, String targetDirOrProp, String filesProp) throws Exception {
    transfer(exchange, targetDirOrProp, filesProp, Operation.MOVE);
  }

  // ---------------- internals ----------------

  private enum Operation { COPY, MOVE }

  private static void transfer(Exchange exchange,
                               String targetDirOrProp,
                               String filesProp,
                               Operation op) throws Exception {

    Objects.requireNonNull(exchange, "exchange");
    Objects.requireNonNull(targetDirOrProp, "targetDirOrProp");
    Objects.requireNonNull(filesProp, "filesProp");

    // Resolve destination directory:
    // - if exchange has property with that name -> use it
    // - else assume it's a literal path
    String destDirStr = exchange.getProperty(targetDirOrProp, String.class);
    if (destDirStr == null || destDirStr.isBlank()) {
      destDirStr = targetDirOrProp;
    }

    // If destDirStr is relative, we resolve relative to inputDir if present
    Path destDir = Paths.get(destDirStr);
    if (!destDir.isAbsolute()) {
      String inputDir = exchange.getProperty("inputDir", String.class);
      if (inputDir != null && !inputDir.isBlank()) {
        destDir = Paths.get(inputDir).resolve(destDirStr);
      }
    }

    Files.createDirectories(destDir);

    // Resolve files from property
    Object raw = exchange.getProperty(filesProp);
    List<Path> files = normalizeToPaths(raw);

    if (files.isEmpty()) return;

    for (Path src : files) {
      if (src == null) continue;

      Path realSrc = src;
      // If it's a relative path and we have inputDir, resolve it
      if (!realSrc.isAbsolute()) {
        String inputDir = exchange.getProperty("inputDir", String.class);
        if (inputDir != null && !inputDir.isBlank()) {
          realSrc = Paths.get(inputDir).resolve(realSrc);
        }
      }

      if (!Files.exists(realSrc)) {
        // File may already have been moved by another step; skip silently
        continue;
      }

      Path dest = destDir.resolve(realSrc.getFileName());

      if (op == Operation.COPY) {
        Files.copy(realSrc, dest, StandardCopyOption.REPLACE_EXISTING);
      } else {
        Files.move(realSrc, dest, StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Path> normalizeToPaths(Object raw) {
    if (raw == null) return List.of();

    // Single Path
    if (raw instanceof Path p) {
      return List.of(p);
    }

    // Single String
    if (raw instanceof String s) {
      String trimmed = s.trim();
      if (trimmed.isEmpty()) return List.of();

      // Support newline-separated lists
      if (trimmed.contains("\n")) {
        String[] parts = trimmed.split("\\R+");
        List<Path> list = new ArrayList<>();
        for (String part : parts) {
          String t = part.trim();
          if (!t.isEmpty()) list.add(Paths.get(t));
        }
        return list;
      }

      return List.of(Paths.get(trimmed));
    }

    // Collection of Path/String
    if (raw instanceof Collection<?> col) {
      List<Path> list = new ArrayList<>();
      for (Object o : col) {
        if (o == null) continue;
        if (o instanceof Path p) list.add(p);
        else if (o instanceof String s) {
          String t = s.trim();
          if (!t.isEmpty()) list.add(Paths.get(t));
        } else {
          // Unknown element type, ignore
        }
      }
      return list;
    }

    // Fallback: unknown type
    return List.of();
  }
}
