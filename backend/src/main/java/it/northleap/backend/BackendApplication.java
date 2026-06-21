package it.northleap.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableAsync/@EnableScheduling: necessari per il workflow engine (Fase 5) - esecuzione
// asincrona dei workflow (config/AsyncConfig.java) e il trigger schedulato orario
// (services/WorkflowScheduler.java).
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
