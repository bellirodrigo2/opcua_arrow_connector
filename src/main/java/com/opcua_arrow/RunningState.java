package com.opcua_arrow;

import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Singleton;

/**
 * Shared running state for the application lifecycle.
 * Used by Context to signal start/stop and by registries to auto-start new
 * tasks.
 */
@Singleton
public class RunningState {

    private final AtomicBoolean running = new AtomicBoolean(false);

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean value) {
        running.set(value);
    }
}
