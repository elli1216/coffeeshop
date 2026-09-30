package com.accenture.cofeeshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tools.jackson.databind.introspect.AnnotatedMember;

@SpringBootApplication
public class CofeeshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(CofeeshopApplication.class, args);
	}
}
