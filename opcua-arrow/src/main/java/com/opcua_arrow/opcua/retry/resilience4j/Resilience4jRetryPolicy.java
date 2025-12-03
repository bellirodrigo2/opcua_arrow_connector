package com.opcua_arrow.opcua.retry.resilience4j;

import java.util.concurrent.Callable;

import com.opcua_arrow.config.RetryPolicyConfig;
import com.opcua_arrow.opcua.retry.IRetryPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

/**
 * Retry policy síncrona utilizando Resilience4j.
 * Perfeita para uso dentro de virtual threads (Java 21).
 */
public class Resilience4jRetryPolicy implements IRetryPolicy, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Resilience4jRetryPolicy.class);

    private final Retry retry;
    private final int maxAttempts;
    private volatile boolean closed = false;

    public Resilience4jRetryPolicy(RetryPolicyConfig config) {
        this.maxAttempts = config.getMaxAttempts();

        RetryConfig.Builder<Object> builder = RetryConfig.custom()
                .maxAttempts(config.getMaxAttempts())
                .waitDuration(config.getInitialDelay());

        // Backoff personalizado (exponential + jitter)
        if (config.isUseJitter()) {
            builder.intervalBiFunction((attempt, outcome) -> {
                long delay = config.getInitialDelay().toMillis();

                for (int i = 1; i < attempt; i++) {
                    delay = (long) (delay * config.getBackoffMultiplier());
                }

                delay = Math.min(delay, config.getMaxDelay().toMillis());

                // jitter completo
                return (long) (Math.random() * delay);
            });
        } else {
            builder.intervalFunction(attempt -> {
                long delay = config.getInitialDelay().toMillis();

                for (int i = 1; i < attempt; i++) {
                    delay = (long) (delay * config.getBackoffMultiplier());
                }

                return Math.min(delay, config.getMaxDelay().toMillis());
            });
        }

        RetryConfig retryConfig = builder.build();
        RetryRegistry registry = RetryRegistry.of(retryConfig);

        this.retry = registry.retry("opcua-retry");

        // Log de tentativas
        retry.getEventPublisher()
                .onRetry(event -> logger.debug("Retry attempt {} after exception: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "unknown"));
    }

    /**
     * Implementação síncrona do retry.
     * Ideal para uso dentro de virtual threads.
     */
    @Override
    public <T> T executeWithRetry(Callable<T> action) {
        if (closed) {
            throw new IllegalStateException("Retry policy is closed");
        }

        Callable<T> decorated = Retry.decorateCallable(retry, action);

        try {
            return decorated.call();
        } catch (Exception e) {
            throw new RuntimeException("Retry attempts exhausted", e);
        }
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    public void close() {
        closed = true;
    }
}
