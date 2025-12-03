package com.opcua_arrow.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.opcua_arrow.IContext;
import com.opcua_arrow.IDataPointFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for loading and hot-reloading data point configurations.
 *
 * Now optimized for Java 21:
 * - Virtual threads for non-blocking periodic work
 * - Scheduler with anti-parallel lock to avoid overlapping executions
 */
@Singleton
public class DataPointLoader {

    private static final Logger logger = LoggerFactory.getLogger(DataPointLoader.class);

    private final IProvideDataPoint<DataPointDTO> dataProvider;
    private final IContext context;
    private final IDataPointFactory<DataPointDTO> factory;
    private final String sourceName;
    private final long intervalSeconds;

    // Scheduler = 1 real thread for timing only
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Factory for virtual threads
    private static final ThreadFactory VIRTUAL_FACTORY = Thread.ofVirtual().name("dp-loader-", 0).factory();

    // Prevents 2 reload cycles from running concurrently
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Instant lastUpdate = Instant.EPOCH;

    @Inject
    public DataPointLoader(
            IProvideDataPoint<DataPointDTO> dataProvider,
            IContext context,
            IDataPointFactory<DataPointDTO> factory,
            @Named("sourceName") String sourceName,
            @Named("updateIntervalSeconds") long intervalSeconds) {

        this.dataProvider = dataProvider;
        this.context = context;
        this.factory = factory;
        this.sourceName = sourceName;
        this.intervalSeconds = intervalSeconds;
    }

    /**
     * Load initial configuration and start hot-reload scheduler
     */
    public void initialize() {
        logger.info("Loading initial data point configuration...");

        try {
            List<DataPointDTO> dataPoints = dataProvider.getDataPoints(sourceName);
            logger.info("Found {} initial data points", dataPoints.size());

            for (DataPointDTO dto : dataPoints) {
                context.update(factory.createDataPoint(dto));
            }

            lastUpdate = Instant.now();

            logger.info("Initial configuration loaded successfully");

            startHotReloadScheduler();

        } catch (Exception e) {
            logger.error("Failed to load initial configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }

    /**
     * Start periodic configuration updates.
     *
     * Uses virtual threads for execution.
     */
    private void startHotReloadScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            // Run update cycle in a virtual thread,
            // but only if not already running
            if (running.compareAndSet(false, true)) {
                VIRTUAL_FACTORY.newThread(this::checkForUpdates).start();
            } else {
                logger.warn("Previous configuration check still running. Skipping this cycle.");
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

        logger.info("Hot-reload scheduler started ({} second intervals)", intervalSeconds);
    }

    /**
     * Check for configuration updates.
     *
     * Runs in a virtual thread. Anti-parallel lock ensures only one execution
     * is active at a time.
     */
    private void checkForUpdates() {
        try {
            Instant checkTime = Instant.now();

            List<DataPointDTO> updated = dataProvider.getUpdatedDataPoints(sourceName, lastUpdate);

            if (!updated.isEmpty()) {
                logger.info("Found {} updated data points", updated.size());
                for (DataPointDTO dto : updated) {
                    context.update(factory.createDataPoint(dto));
                }
            }

            List<String> deleted = dataProvider.getDeletedDataPoints(sourceName, lastUpdate);

            if (!deleted.isEmpty()) {
                logger.info("Found {} deleted data points", deleted.size());
                for (String nodeId : deleted) {
                    context.delete(nodeId);
                }
            }

            lastUpdate = checkTime;

            if (!updated.isEmpty() || !deleted.isEmpty()) {
                logger.info(
                        "Configuration hot-reload completed: {} updates, {} deletions",
                        updated.size(), deleted.size());
            }

        } catch (Exception e) {
            logger.error("Error during configuration update check", e);

        } finally {
            // Always release lock
            running.set(false);
        }
    }

    /**
     * Shutdown scheduler gracefully
     */
    public void shutdown() {
        logger.info("Shutting down DataPointLoader...");
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("DataPointLoader shutdown complete");
    }
}
