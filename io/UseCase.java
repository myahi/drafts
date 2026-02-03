Voici des exemples concrets (Spring Boot + Camel) pour utiliser tes utilitaires / FileIOService.
Je te donne 4 scénarios typiques : merge, zip, unzip, listing + tri + routage, + taille.


---

1) Merge de fichiers (inputs en header, output en header)

from("direct:mergeFiles")
  // headers attendus: outPath (String), inputs (List<String>)
  .process(e -> {
    var out = Path.of(e.getIn().getHeader("outPath", String.class));
    var inputs = e.getIn().getHeader("inputs", List.class);
    @SuppressWarnings("unchecked")
    var paths = (List<String>) inputs;
    e.getIn().setHeader("out", out);
    e.getIn().setHeader("inPaths", paths.stream().map(Path::of).toList());
  })
  .bean(FileIOService.class, "merge(${header.out}, ${header.inPaths})");

✅ Usage : tu passes depuis un autre route :

template.sendBodyAndHeaders("direct:mergeFiles", null, Map.of(
  "outPath", "/data/out/merged.txt",
  "inputs", List.of("/data/in/a.txt", "/data/in/b.txt")
));


---

2) Zip de fichiers (inputs en body, output en header)

from("direct:zipFiles")
  // body: List<String> ; header: zipOutPath
  .process(e -> {
    @SuppressWarnings("unchecked")
    var inputs = (List<String>) e.getIn().getBody(List.class);
    e.getIn().setHeader("inPaths", inputs.stream().map(Path::of).toList());
    e.getIn().setHeader("zipOut", Path.of(e.getIn().getHeader("zipOutPath", String.class)));
  })
  .bean(FileIOService.class, "zip(${header.zipOut}, ${header.inPaths}, true)");


---

3) Unzip vers un dossier (et sécurisation zip-slip déjà dans util)

> Dans ton code précédent, FileIOService n’exposait pas unzip :
Ajoute juste dans FileIOService :



public void unzip(Path zipFile, Path destDir, boolean overwrite) throws Exception {
  ZipUtil.unzip(zipFile, destDir, props.getBufferSize(), overwrite);
}

Puis route Camel :

from("direct:unzip")
  // headers: zipPath, destDir
  .process(e -> {
    e.getIn().setHeader("zip", Path.of(e.getIn().getHeader("zipPath", String.class)));
    e.getIn().setHeader("dest", Path.of(e.getIn().getHeader("destDir", String.class)));
  })
  .bean(FileIOService.class, "unzip(${header.zip}, ${header.dest}, true)");


---

4) Listing + tri + routage Camel (ex: traiter les plus gros d’abord)

from("timer:listEveryMinute?period=60000")
  .setHeader("dir").constant("/data/in")
  .process(e -> {
    Path dir = Path.of(e.getIn().getHeader("dir", String.class));
    var files = FileListingUtil.listFiles(
        dir,
        true,
        FileListingUtil.SortCriterion.SIZE,
        FileListingUtil.SortDirection.DESC
    );
    e.getIn().setBody(files);
  })
  .split(body()) // chaque Path
    .process(e -> {
      Path p = e.getIn().getBody(Path.class);
      e.getIn().setHeader("filePath", p.toString());
      e.getIn().setHeader("sizeMb", FileSizeUtil.size(p, FileSizeUnit.MB));
    })
    .choice()
      .when(header("sizeMb").isGreaterThan(50))
        .to("direct:bigFileFlow")
      .otherwise()
        .to("direct:smallFileFlow")
    .endChoice()
  .end();


---

5) Exemple taille fichier “log friendly”

from("direct:logFileSize")
  .process(e -> {
    Path p = Path.of(e.getIn().getHeader("filePath", String.class));
    long sizeMb = FileSizeUtil.size(p, FileSizeUnit.MB);
    e.getIn().setHeader("sizeMb", sizeMb);
  })
  .log("Fichier ${header.filePath} = ${header.sizeMb} MB");


---

6) Exemple complet “pipeline” (list -> zip -> checksum)

from("direct:archiveDaily")
  .setHeader("dir").constant("/data/in")
  .setHeader("zipOut").simple("/data/out/archive-${date:now:yyyyMMdd}.zip")
  .process(e -> {
    Path dir = Path.of(e.getIn().getHeader("dir", String.class));
    var files = FileListingUtil.listFiles(dir, false,
        FileListingUtil.SortCriterion.NAME,
        FileListingUtil.SortDirection.ASC);
    e.getIn().setHeader("inPaths", files);
    e.getIn().setHeader("zipPath", Path.of(e.getIn().getHeader("zipOut", String.class)));
  })
  .bean(FileIOService.class, "zip(${header.zipPath}, ${header.inPaths}, true)")
  .process(e -> {
    Path zip = e.getIn().getHeader("zipPath", Path.class);
    String sha = ChecksumUtil.sha256(zip); // ou via service si tu exposes sha256(Path)
    e.getIn().setHeader("zipSha256", sha);
  })
  .log("Archive créée: ${header.zipOut} sha256=${header.zipSha256}");


---

Tu veux des exemples plus “Camel idiomatiques” ?

Dis-moi juste comment tu passes tes infos aujourd’hui :

les fichiers sont dans headers (CamelFileName / CamelFilePath) ?

ou tu utilises file: / ftp: / sftp: endpoints ?

tu veux appeler le service via bean(...) ou to("bean:...") ?


Et je te donne 2 routes prêtes à copier/coller adaptées à ton style (avec file: consumer + move + idempotent, etc.).
