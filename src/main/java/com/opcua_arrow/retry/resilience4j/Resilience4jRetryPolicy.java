package com.opcua_arrow.retry.resilience4j;

import com.opcua_arrow.config.RetryPolicyConfig;
import com.opcua_arrow.retry.IRetryPolicy;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Retry policy implementation using Resilience4j.
 */
public class Resilience4jRetryPolicy implements IRetryPolicy, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Resilience4jRetryPolicy.class);
    
    private static final ScheduledExecutorService SHARED_SCHEDULER = createSharedScheduler();
    
    private final Retry retry;
    private final int maxAttempts;
    private volatile boolean closed = false;
    
    private static ScheduledExecutorService createSharedScheduler() {
        ThreadFactory daemonThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "resilience4j-retry-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
        return Executors.newScheduledThreadPool(2, daemonThreadFactory);
    }
    
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
                long finalDelay = (long) (Math.random() * delay);
                return finalDelay;
            });
        } else {
            builder.intervalFunction(attempt -> {
                long delay = config.getInitialDelay().toMillis();
                for (int i = 1; i < attempt; i++) {
                    delay = (long) (delay * config.getBackoffMultiplier());
                }
                long finalDelay = Math.min(delay, config.getMaxDelay().toMillis());
                return finalDelay;
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
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("Retry policy is closed"));
        }
        Supplier<java.util.concurrent.CompletionStage<T>> stageSupplier = () -> operation.get();
        return Retry.decorateCompletionStage(retry, SHARED_SCHEDULER, stageSupplier)
            .get()
            .toCompletableFuture();
    }
    
    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    @Override
    public void close() {
        closed = true;
    }
    
    public static void shutdownSharedScheduler() {
        SHARED_SCHEDULER.shutdown();
        try {
            if (!SHARED_SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                SHARED_SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            SHARED_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
