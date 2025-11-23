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
    
    /**
     * Create configuration from environment variables
     */
    public static RetryPolicyConfig fromEnvironment() {
        RetryPolicyConfig config = new RetryPolicyConfig();
        
        String maxAttempts = System.getenv("RETRY_MAX_ATTEMPTS");
        if (maxAttempts != null) {
            config.setMaxAttempts(Integer.parseInt(maxAttempts));
        }
        
        String initialDelay = System.getenv("RETRY_INITIAL_DELAY_MS");
        if (initialDelay != null) {
            config.setInitialDelay(Duration.ofMillis(Long.parseLong(initialDelay)));
        }
        
        String maxDelay = System.getenv("RETRY_MAX_DELAY_MS");
        if (maxDelay != null) {
            config.setMaxDelay(Duration.ofMillis(Long.parseLong(maxDelay)));
        }
        
        String backoffMultiplier = System.getenv("RETRY_BACKOFF_MULTIPLIER");
        if (backoffMultiplier != null) {
            config.setBackoffMultiplier(Double.parseDouble(backoffMultiplier));
        }
        
        String useJitter = System.getenv("RETRY_USE_JITTER");
        if (useJitter != null) {
            config.setUseJitter(Boolean.parseBoolean(useJitter));
        }
        
        return config;
    }
    
    /**
     * Validate configuration
     */
    public void validate() {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max attempts must be positive");
        }
        if (initialDelay.isNegative() || initialDelay.isZero()) {
            throw new IllegalArgumentException("Initial delay must be positive");
        }
        if (maxDelay.isNegative() || maxDelay.compareTo(initialDelay) < 0) {
            throw new IllegalArgumentException("Max delay must be greater than or equal to initial delay");
        }
        if (backoffMultiplier <= 1.0) {
            throw new IllegalArgumentException("Backoff multiplier must be greater than 1.0");
        }
    }
    
    @Override
    public String toString() {
        return "RetryPolicyConfig{" +
                "maxAttempts=" + maxAttempts +
                ", initialDelay=" + initialDelay +
                ", maxDelay=" + maxDelay +
                ", backoffMultiplier=" + backoffMultiplier +
                ", useJitter=" + useJitter +
                '}';
    }
}
