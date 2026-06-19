package com.finassistmini.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Configuration
public class AppConfig {

    @Bean
    ApplicationRunner dataDirectoryInitializer(FinassistProperties properties) {
        return args -> {
            Files.createDirectories(properties.docsDirectory());
            PathSupport.createParentDirectories(properties.vectorStoreFile());
        };
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService ingestionExecutor(FinassistProperties properties) {
        return Executors.newFixedThreadPool(properties.uploadMaxConcurrency());
    }

    @Bean
    Semaphore uploadSemaphore(FinassistProperties properties) {
        return new Semaphore(properties.uploadMaxConcurrency());
    }

    @Bean
    Semaphore chatSemaphore(FinassistProperties properties) {
        return new Semaphore(properties.chatMaxConcurrency());
    }
}
