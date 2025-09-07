package com.deeptruth.deeptruth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeeptruthApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeeptruthApplication.class, args);
	}

}
