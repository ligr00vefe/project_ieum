package com.project.ieum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IeumApplication {

	public static void main(String[] args) {
        SpringApplication.run(IeumApplication.class, args);
        System.out.println("http://localhost:8080/ieum");
	}

}
