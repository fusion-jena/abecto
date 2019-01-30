package de.uni_jena.cs.fusion.abecto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Abecto {

	public static void main(String[] args) {
		SpringApplication.exit(SpringApplication.run(Abecto.class, args));
	}

}