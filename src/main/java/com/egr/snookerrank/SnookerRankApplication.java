package com.egr.snookerrank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SnookerRankApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnookerRankApplication.class, args);
	}

}
