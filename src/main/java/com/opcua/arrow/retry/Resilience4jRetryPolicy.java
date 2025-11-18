package com.opcua.arrow.retry;

import com.opcua.arrow.config.RetryPolicyConfig;
import com.opcua.arrow.interfaces.IRetryPolicy;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Retry policy implementation using Resilience4j.
 */
public class Resilience4jRetryPolicy implements IRetryPolicy {
    private static final Logger logger = LoggerFactory.getLogger(Resilience4jRetryPolicy.class);
    
    private final Retry retry;
    private final int maxAttempts;
    
    public Resilience4jRetryPolicy(RetryPolicyConfig config) {
        this.maxAttempts = config.getMaxAttempts();
        
        RetryConfig.Builder<Object> builder = RetryConfig.custom()
            .maxAttempts(config.getMaxAttempts())
            .waitDuration(config.getInitialDelay());
        
        // Configure exponential backoff with jitter if enabled
        if (config.isUseJitter()) {
            builder.intervalBiFunction((attempt, outcome) -> {
                long delay = config.getInitialDelay().toMillis();
                for (int i = 1; i < attempt; i++) {
                    delay = (long) (delay * config.getBackoffMultiplier());
                }
                delay = Math.min(delay, config.getMaxDelay().toMillis());
                
                // Apply full jitter
                return Duration.ofMillis((long) (Math.random() * delay));
            });
        } else {
            builder.intervalFunction(attempt -> {
                long delay = config.getInitialDelay().toMillis();
                for (int i = 1; i < attempt; i++) {
                    delay = (long) (delay * config.getBackoffMultiplier());
                }
                return Duration.ofMillis(Math.min(delay, config.getMaxDelay().toMillis()));
            });
        }
        
        RetryConfig retryConfig = builder.build();
        RetryRegistry registry = RetryRegistry.of(retryConfig);
        this.retry = registry.retry("opcua-retry");
        
        // Add event listeners for logging
        retry.getEventPublisher()
            .onRetry(event -> logger.debug("Retry attempt {} after exception: {}", 
                event.getNumberOfRetryAttempts(), 
                event.getLastThrowable().getMessage()));
    }
    
    @Override
    public <T> CompletableFuture<T> executeWithRetry(Supplier<CompletableFuture<T>> operation) {
        return Retry.decorateCompletionStage(retry, () -> operation.get())
            .get()
            .toCompletableFuture();
    }
    
    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
