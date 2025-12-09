import java.util.ArrayList;

/**
 * Custom HashMap implementation using ArrayList of buckets.
 * Uses separate chaining for collision resolution and automatic rehashing
 * when load factor exceeds 0.75.
 */
public class CustomHashMap<K, V> {
    /**
     * Internal entry class representing a key-value pair in the hash map.
     */
    private static class Entry<K, V> {
        K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    // Array of buckets, each bucket is a list of entries (chaining)
    private ArrayList<ArrayList<Entry<K, V>>> buckets;

    // Number of key-value pairs currently stored
    private int size;

    // Starting capacity for the hash table
    private static final int INITIAL_CAPACITY = 16;

    /**
     * Create a new empty hash map with initial capacity.
     */
    public CustomHashMap() {
        buckets = new ArrayList<>();
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            buckets.add(new ArrayList<>());
        }
        size = 0;
    }

    /**
     * Calculate the bucket index for a given key using its hash code.
     * Uses modulo to map hash codes to valid bucket indices.
     */
    private int getBucketIndex(K key) {
        int hashCode = key.hashCode();
        return Math.abs(hashCode) % buckets.size();
    }

    /**
     * Insert or update a key-value pair in the hash map.
     * If key exists, updates its value. Otherwise, adds new entry.
     * Triggers rehashing if load factor exceeds 0.75.
     */
    public void put(K key, V value) {
        int bucketIndex = getBucketIndex(key);
        ArrayList<Entry<K, V>> bucket = buckets.get(bucketIndex);

        // Check if key already exists and update if found
        for (int i = 0; i < bucket.size(); i++) {
            Entry<K, V> entry = bucket.get(i);
            if (entry.key.equals(key)) {
                entry.value = value;
                return;
            }
        }

        // Add new entry to bucket
        bucket.add(new Entry<>(key, value));
        size++;

        // Rehash if load factor exceeds threshold
        if (size > buckets.size() * 0.75) {
            rehash();
        }
    }

    /**
     * Retrieve the value associated with a key.
     * Returns null if key is not found.
     */
    public V get(K key) {
        int bucketIndex = getBucketIndex(key);
        ArrayList<Entry<K, V>> bucket = buckets.get(bucketIndex);

        for (int i = 0; i < bucket.size(); i++) {
            Entry<K, V> entry = bucket.get(i);
            if (entry.key.equals(key)) {
                return entry.value;
            }
        }

        return null;
    }

    /**
     * Check if the hash map contains a specific key.
     */
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    /**
     * Remove a key-value pair from the hash map.
     * Returns the removed value, or null if key was not found.
     */
    public V remove(K key) {
        int bucketIndex = getBucketIndex(key);
        ArrayList<Entry<K, V>> bucket = buckets.get(bucketIndex);

        for (int i = 0; i < bucket.size(); i++) {
            Entry<K, V> entry = bucket.get(i);
            if (entry.key.equals(key)) {
                V value = entry.value;
                bucket.remove(i);
                size--;
                return value;
            }
        }

        return null;
    }

    /**
     * Get a list of all keys in the hash map.
     * Order is not guaranteed.
     */
    public ArrayList<K> keySet() {
        ArrayList<K> keys = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            ArrayList<Entry<K, V>> bucket = buckets.get(i);
            for (int j = 0; j < bucket.size(); j++) {
                keys.add(bucket.get(j).key);
            }
        }
        return keys;
    }

    /**
     * Get a list of all values in the hash map.
     * Order is not guaranteed.
     */
    public ArrayList<V> values() {
        ArrayList<V> vals = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            ArrayList<Entry<K, V>> bucket = buckets.get(i);
            for (int j = 0; j < bucket.size(); j++) {
                vals.add(bucket.get(j).value);
            }
        }
        return vals;
    }

    /**
     * Get a list of all entries in the hash map.
     * Order is not guaranteed.
     */
    public ArrayList<Entry<K, V>> entrySet() {
        ArrayList<Entry<K, V>> entries = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            ArrayList<Entry<K, V>> bucket = buckets.get(i);
            for (int j = 0; j < bucket.size(); j++) {
                entries.add(bucket.get(j));
            }
        }
        return entries;
    }

    /**
     * Remove all entries and reset to initial capacity.
     */
    public void clear() {
        buckets.clear();
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            buckets.add(new ArrayList<>());
        }
        size = 0;
    }

    /**
     * Double the capacity and redistribute all entries.
     * Called automatically when load factor exceeds 0.75 to maintain performance.
     */
    private void rehash() {
        ArrayList<ArrayList<Entry<K, V>>> oldBuckets = buckets;
        buckets = new ArrayList<>();
        int newCapacity = oldBuckets.size() * 2;

        // Create new bucket array with doubled capacity
        for (int i = 0; i < newCapacity; i++) {
            buckets.add(new ArrayList<>());
        }

        size = 0;

        // Reinsert all entries into new buckets
        for (int i = 0; i < oldBuckets.size(); i++) {
            ArrayList<Entry<K, V>> bucket = oldBuckets.get(i);
            for (int j = 0; j < bucket.size(); j++) {
                Entry<K, V> entry = bucket.get(j);
                put(entry.key, entry.value);
            }
        }
    }

    /**
     * Public wrapper class for exposing map entries externally.
     * Provides getter methods for key and value.
     */
    public static class MapEntry<K, V> {
        private K key;
        private V value;

        public MapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}