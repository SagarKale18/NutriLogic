package com.nutrilogic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // This is the only line you need to add
public class NutrilogicApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutrilogicApplication.class, args);
    }
}