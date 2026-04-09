package com.pience.padobox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.pience.padobox.controller.DefaultController;

@SpringBootApplication
public class PadoboxApiV1Application {

	public static void main(String[] args) {
		SpringApplication.run(DefaultController.class, args);
	}

}
