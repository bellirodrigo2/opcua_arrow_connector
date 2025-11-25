package com.opcua_arrow.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.opcua_arrow.config.PostgreSQLConfig;
import com.opcua_arrow.context.Context;
import com.opcua_arrow.data.DataPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for loading and hot-reloading data point configurations
 */
@Singleton
public class DataPointLoader {

    private static final Logger logger = LoggerFactory.getLogger(DataPointLoader.class);

    private final IProvideDataPoint dataProvider;
    private final Context context;
    private final PostgreSQLConfig config;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private Instant lastUpdate = Instant.EPOCH;

    @Inject
    public DataPointLoader(IProvideDataPoint dataProvider, Context context, PostgreSQLConfig config) {
        this.dataProvider = dataProvider;
        this.context = context;
        this.config = config;
    }

    /**
     * Load initial configuration and start hot-reload scheduler
     */
    public void initialize() {
        logger.info("Loading initial data point configuration...");

        try {
            // Load all initial data points
            List<DataPointDTO> dataPoints = dataProvider.getDataPoints();
            logger.info("Found {} initial data points", dataPoints.size());

            for (DataPointDTO dto : dataPoints) {
                DataPoint params = DataPoint.fromConfig(dto);
                context.update(params);
            }

            // Update timestamp
            lastUpdate = Instant.now();

            logger.info("Initial configuration loaded successfully");

            // Start hot-reload scheduler (every 30 seconds)
            startHotReloadScheduler();

        } catch (Exception e) {
            logger.error("Failed to load initial configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }

    /**
     * Start periodic configuration updates
     */
    private void startHotReloadScheduler() {
        if (!config.isEnableHotReload()) {
            logger.info("Hot-reload is disabled");
            return;
        }

        long intervalSeconds = config.getHotReloadInterval().toSeconds();
        scheduler.scheduleAtFixedRate(this::checkForUpdates, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        logger.info("Hot-reload scheduler started ({} second intervals)", intervalSeconds);
    }

    /**
     * Check for configuration updates
     */
    private void checkForUpdates() {
        try {
            Instant checkTime = Instant.now();

            // Check for updated data points
            List<DataPointDTO> updated = dataProvider.getUpdatedDataPoints(lastUpdate);
            if (!updated.isEmpty()) {
                logger.info("Found {} updated data points", updated.size());
                for (DataPointDTO dto : updated) {
                    DataPoint params = DataPoint.fromConfig(dto);
                    context.update(params);
                }
            }

            // Check for deleted data points
            List<String> deleted = dataProvider.getDeletedDataPoints(lastUpdate);
            if (!deleted.isEmpty()) {
                logger.info("Found {} deleted data points", deleted.size());
                for (String nodeId : deleted) {
                    context.delete(nodeId);
                }
            }

            // Update timestamp
            lastUpdate = checkTime;

            if (!updated.isEmpty() || !deleted.isEmpty()) {
                logger.info("Configuration hot-reload completed: {} updates, {} deletions",
                        updated.size(), deleted.size());
            }

        } catch (Exception e) {
            logger.error("Error during configuration update check", e);
            // Continue running - don't stop the scheduler for transient errors
        }
    }

    /**
     * Stop the hot-reload scheduler
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
