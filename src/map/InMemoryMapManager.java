package map;

import core.StationData;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryMapManager {
    private final ConcurrentHashMap<Character, StationData> map = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void update(String station, double temp) {
        char c = Character.toLowerCase(station.charAt(0));
        lock.writeLock().lock();
        try {
            map.computeIfAbsent(c, k -> new StationData()).update(temp);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<Character, StationData> snapshot() {
        lock.readLock().lock();
        try {
            return new TreeMap<>(map); // Sorted by key
        } finally {
            lock.readLock().unlock();
        }
    }

    public void reset() {
        lock.writeLock().lock();
        try {
            map.clear();
            System.out.println("In-memory map reset.");
        } finally {
            lock.writeLock().unlock();
        }
    }
}
