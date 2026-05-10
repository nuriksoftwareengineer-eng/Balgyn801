package com.nurba.java;

import com.nurba.java.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class Balgyn801Application {

    public static void main(String[] args) {
        SpringApplication.run(Balgyn801Application.class, args);
    }

}
