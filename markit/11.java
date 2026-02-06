package fr.lbp.markit.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import fr.lbp.markit.controller.LbpMarkitClient;

@ComponentScan("fr.lbp")
@SpringBootApplication
public class MarkitApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MarkitApplication.class, args);

        // ✅ IMPORTANT : une seule fois, au démarrage
        LbpMarkitClient.setApplicationContext(ctx);
    }
} 
