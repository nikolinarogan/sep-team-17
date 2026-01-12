package com.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankSimulatorApplication {

	public static void main(String[] args) {
		String projectDir = System.getProperty("user.dir");
		String trustStorePath = projectDir + "\\src\\main\\resources\\truststore.jks";
		System.out.println("📂 Pokušavam da učitam truststore sa: " + trustStorePath);

		// 2. Postavljamo sistemska svojstva
		System.setProperty("javax.net.ssl.trustStore", trustStorePath);
		System.setProperty("javax.net.ssl.trustStorePassword", "lozinka123");
		SpringApplication.run(BankSimulatorApplication.class, args);
	}

}
