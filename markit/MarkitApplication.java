package fr.lbp.markit.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("fr.lbp")
@SpringBootApplication
public class MarkitApplication {
	public static void main(String[] args) {
		SpringApplication.run(MarkitApplication.class, args);
	}
}
