package com.opcua_arrow.opcua.retry;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Interface for retry policy implementations.
 */
public interface IRetryPolicy {

    /**
     * Executes an async operation with retry logic.
     *
     * @param <T>       The type of the result
     * @param operation The operation to execute
     * @return A future containing the result
     */
    <T> CompletableFuture<T> executeWithRetry(Supplier<CompletableFuture<T>> operation);

    /**
     * Gets the maximum number of attempts.
     *
     * @return The maximum attempts
     */
    int getMaxAttempts();
}
