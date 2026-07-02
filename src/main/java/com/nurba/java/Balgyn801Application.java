package com.nurba.java;

import com.nurba.java.config.DotenvLoader;
import com.nurba.java.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
@EnableScheduling
@EnableAsync
public class Balgyn801Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Balgyn801Application.class);
        // Load .env before Spring reads application.properties.
        // setDefaultProperties has the lowest priority in Spring's source chain, so OS
        // environment variables, system properties (-D), and application.properties all win.
        app.setDefaultProperties(DotenvLoader.load());
        app.run(args);
    }

}
