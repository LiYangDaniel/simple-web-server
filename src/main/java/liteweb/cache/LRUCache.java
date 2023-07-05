package liteweb.cache;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<K, V> {

    private final int capacity;
    private final LinkedHashMap<K, V> cache;
    private final Lock lock;
    private final Condition write;
    private volatile boolean isWriting = false;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<K, V>(this.capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
        this.lock = new ReentrantLock();
        this.write = lock.newCondition();
    }

    public void put(K key, V value) throws InterruptedException {
        lock.lock();
        try {
            while (isWriting) {
                write.await();
            }
            isWriting = true;
            cache.put(key, value);
            isWriting = false;
        } finally {
            write.signalAll();
            lock.unlock();
        }
    }

    public V get(K key) throws InterruptedException {
        lock.lock();
        try {
            while (isWriting) {
                write.await();
            }
            return cache.get(key);
        } finally {
            lock.unlock();
        }
    }
}