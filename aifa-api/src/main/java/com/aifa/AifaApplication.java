package com.aifa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AifaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AifaApplication.class, args);
    }
}
