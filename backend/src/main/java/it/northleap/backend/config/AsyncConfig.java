package it.northleap.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// magari passare direttamente a redis
@Configuration
public class AsyncConfig {
    // permetto di eseguire più operazioni pesanti insieme in maniera asincrona
    // l'idea è quella di sostituire un redis, salvando in ram
    @Bean(name = "workflowTaskExecutor")
    public ThreadPoolTaskExecutor workflowTaskExecutor(
            // 2 thread base, massimo di coda 100, mentre thread extra, 8
            @Value("${app.workflow.executor.core-pool-size:2}") int corePoolSize,
            @Value("${app.workflow.executor.max-pool-size:8}") int maxPoolSize,
            @Value("${app.workflow.executor.queue-capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("workflow-");
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }
}
