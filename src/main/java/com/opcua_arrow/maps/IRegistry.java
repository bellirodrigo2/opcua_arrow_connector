package com.opcua_arrow.maps;

import java.util.Collection;

/**
 * Generic registry interface for managing key-value mappings with thread-safe operations.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public interface IRegistry<K, V> {

    /**
     * Returns the value associated with the specified key, or null if not found.
     */
    V get(K key);

    /**
     * Removes the mapping for the specified key.
     */
    void remove(K key);

    /**
     * Returns true if this registry contains a mapping for the specified key.
     */
    boolean containsKey(K key);

    /**
     * Returns a collection view of the values contained in this registry.
     */
    Collection<V> values();
}
