package com.opcua_arrow.opcua.retry;

import java.util.concurrent.Callable;

/**
 * Interface for retry policy implementations.
 */
public interface IRetryPolicy {

    /**
     * Executes an async operation with retry logic.
     *
     * @param <T>       The type of the result
     * @param operation The operation to execute
     * @return T result
     */
    <T> T executeWithRetry(Callable<T> operation);

    default void executeVoidWithRetry(Callable<Void> action) {
        executeWithRetry(action);
    }

    /**
     * Gets the maximum number of attempts.
     *
     * @return The maximum attempts
     */
    int getMaxAttempts();
}
