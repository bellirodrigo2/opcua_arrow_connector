package com.opcua_arrow.context;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.opcua_arrow.config.DataValueConfig;
import com.opcua_arrow.factory.transform.FilterFactory;
import com.opcua_arrow.transform.IDataPointEqual;
import com.opcua_arrow.transform.IDataPointParams;
import com.opcua_arrow.transform.opcua.DataPointParams;

public class Context {

    private final Map<String, IDataPointParams> paramsMap;
    private final Map<Long, Set<String>> nodeIdsIntervalMap;
    private final IntervalUpdateCallback callback;

    public Context(Map<String, IDataPointParams> paramsMap,
            List<DataValueConfig> configList,
            IntervalUpdateCallback intervalUpdateCallback) {

        this.paramsMap = ensureConcurrent(paramsMap);
        this.nodeIdsIntervalMap = new ConcurrentHashMap<>();
        this.callback = intervalUpdateCallback;

        for (DataValueConfig config : configList) {
            insertParams(config);
            insertNodeInterval(config);
        }
    }

    private <K, V> Map<K, V> ensureConcurrent(Map<K, V> map) {
        return (map instanceof ConcurrentHashMap)
                ? map
                : new ConcurrentHashMap<>(map);
    }

    // =====================================================================
    // PUBLIC UPDATE — atomic inside each map (not whole method)
    // =====================================================================
    public void update(DataValueConfig config) {
        String nodeId = config.getNodeId();

        Long oldInterval = findOldInterval(nodeId);
        Long newInterval = config.getInterval();

        // always update params
        updateParams(config);

        // only update interval map if interval changed
        if (oldInterval == null || !newInterval.equals(oldInterval)) {
            updateNodeInterval(nodeId, oldInterval, newInterval);
            if (callback != null) {
                callback.onIntervalUpdate(nodeId, oldInterval, newInterval);
            }
        }
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    /** Finds the old interval where this nodeId was registered */
    private Long findOldInterval(String nodeId) {
        for (Map.Entry<Long, Set<String>> entry : nodeIdsIntervalMap.entrySet()) {
            if (entry.getValue().contains(nodeId)) {
                return entry.getKey();
            }
        }
        return null; // NodeID not yet registered
    }

    // =====================================================================
    // PARAM MAP METHODS
    // =====================================================================

    private IDataPointParams createDataPointParams(DataValueConfig config) {
        IDataPointEqual equals = FilterFactory.createFilter(config.getFilter());
        return new DataPointParams(config.getPointId(), equals, config.getGroup());
    }

    private void insertParams(DataValueConfig config) {
        paramsMap.put(config.getNodeId(), createDataPointParams(config));
    }

    /** Atomic update only inside paramsMap */
    private void updateParams(DataValueConfig config) {
        paramsMap.compute(config.getNodeId(), (nodeId, oldParams) -> createDataPointParams(config));
    }

    // =====================================================================
    // INTERVAL MAP METHODS
    // =====================================================================

    private void insertNodeInterval(DataValueConfig config) {
        Long interval = config.getInterval();
        String nodeId = config.getNodeId();

        nodeIdsIntervalMap.compute(interval, (key, set) -> {
            if (set == null) {
                set = ConcurrentHashMap.newKeySet();
            }
            set.add(nodeId);
            return set;
        });
    }

    /** Atomic update inside nodeIdsIntervalMap */
    private void updateNodeInterval(String nodeId, Long oldInterval, Long newInterval) {

        // REMOVE from old interval if exists
        if (oldInterval != null) {
            nodeIdsIntervalMap.computeIfPresent(oldInterval, (key, set) -> {
                set.remove(nodeId);
                return set.isEmpty() ? null : set;
            });
        }

        // ADD to new interval
        nodeIdsIntervalMap.compute(newInterval, (key, set) -> {
            if (set == null) {
                set = ConcurrentHashMap.newKeySet();
            }
            set.add(nodeId);
            return set;
        });
    }
}
