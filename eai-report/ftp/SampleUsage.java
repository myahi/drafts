FtpMessage ftpMessage = FtpMessage.builder()
    .eaiFtpMessage(
        FtpMessage.EaiFtpMessage.builder()
            .header(
                FtpMessage.Header.builder()
                    .action(FtpMessage.Action.send)
                    .connectionName("MY_SFTP_CONNECTION")
                    .timeout(30)
                    .nbMaxRetry("3")
                    .waitForRetry("10")
                    .build()
            )
            .file(
                FtpMessage.File.builder()
                    .name("test.txt")
                    .remoteLocation("/out")
                    .content("contenu du fichier")
                    .build()
            )
            .build()
    )
    .build();
