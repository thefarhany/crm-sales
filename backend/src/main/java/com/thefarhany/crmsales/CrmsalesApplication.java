package com.thefarhany.crmsales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.thefarhany.crmsales.repository")
public class CrmsalesApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrmsalesApplication.class, args);
	}

}
