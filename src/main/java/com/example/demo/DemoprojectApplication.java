package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoprojectApplication {

	private static final Logger log = LoggerFactory.getLogger(DemoprojectApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DemoprojectApplication.class, args);
		log.info("------------------项目启动完成--------------");
	}

}
