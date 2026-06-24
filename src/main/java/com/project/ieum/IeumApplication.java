package com.project.ieum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableAsync
public class IeumApplication {

	private static final Logger log = LoggerFactory.getLogger(IeumApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(IeumApplication.class, args);
		String port = context.getEnvironment().getProperty("server.port", "8080");
		String contextPath = context.getEnvironment().getProperty("server.servlet.context-path", "");
		String basePath = (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) ? "/" : contextPath;
		log.info("IEUM application started: http://localhost:{}{}", port, basePath);
	}
}
