package com.accreditations_service.accreditations_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class AccreditationsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccreditationsServiceApplication.class, args);
	}

}
