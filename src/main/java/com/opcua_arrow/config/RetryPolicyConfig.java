package com.opcua_arrow.config;

import java.time.Duration;

/**
 * Configuration for retry policy.
 */
public class RetryPolicyConfig {
    private int maxAttempts = 3;
    private Duration initialDelay = Duration.ofMillis(500);
    private Duration maxDelay = Duration.ofSeconds(5);
    private double backoffMultiplier = 2.0;
    private boolean useJitter = true;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public RetryPolicyConfig setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public RetryPolicyConfig setInitialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
        return this;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public RetryPolicyConfig setMaxDelay(Duration maxDelay) {
        this.maxDelay = maxDelay;
        return this;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public RetryPolicyConfig setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
        return this;
    }

    public boolean isUseJitter() {
        return useJitter;
    }

    public RetryPolicyConfig setUseJitter(boolean useJitter) {
        this.useJitter = useJitter;
        return this;
    }
}
