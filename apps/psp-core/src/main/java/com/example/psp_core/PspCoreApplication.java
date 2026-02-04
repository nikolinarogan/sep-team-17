package com.example.psp_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.example.psp_core",
		"controller",
		"service",
		"config",
		"dto",
		"repository",
		"tools"
})
@EnableJpaRepositories(basePackages = "repository")
@EnableScheduling
@EntityScan(basePackages = "model")
public class PspCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(PspCoreApplication.class, args);
	}

}