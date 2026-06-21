package it.northleap.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// TaskExecutor dedicato per l'esecuzione asincrona dei workflow (Fase 5, 05-WORKFLOW-ENGINE.md):
// sostituisce BullMQ/Redis dell'originale con @Async + un pool di thread proprio, invece del
// pool di default condiviso da Spring, come esplicitamente richiesto dal doc. Limite consapevole
// (anche questo dal doc): nessuna persistenza della coda stessa - un job non ancora preso da un
// thread si perde se l'app si riavvia.
@Configuration
public class AsyncConfig {

    @Bean(name = "workflowTaskExecutor")
    public ThreadPoolTaskExecutor workflowTaskExecutor(
            @Value("${app.workflow.executor.core-pool-size:2}") int corePoolSize,
            @Value("${app.workflow.executor.max-pool-size:8}") int maxPoolSize,
            @Value("${app.workflow.executor.queue-capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("workflow-");
        executor.initialize();
        return executor;
    }
}
