package DocuStore.db;


import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

class SafeMap<K, V> {
    final private HashMap<K,V> items;
    ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    Lock readLock = readWriteLock.readLock();
    Lock writeLock = readWriteLock.writeLock();

    public SafeMap(){
        items = new HashMap<>();
    }


    public boolean containsKey(K item){
        readLock.lock();
        boolean val = items.containsKey(item);
        readLock.unlock();
        return val;
    }

    /**
     * Add the item to the Set
     * @param item
     * @return - True if the item wasn't already present, false otherwise
     */
    public void put(K key, V item){
        writeLock.lock();
        items.put(key, item);
        writeLock.unlock();
    }

    public V remove(K key){
        writeLock.lock();
        V output = items.remove(key);
        writeLock.unlock();
        return output;
    }

    public V get(K key){
        readLock.lock();
        V val = items.get(key);
        readLock.unlock();
        return val;
    }

    /**
     * Remove a value under a certain condition.
     * @param key - Key of the value to be removed
     * @param method - Conditional, must return boolean
     * @return - Value if removed, null otherwise
     */
    public V removeIf(K key, Function<V, Boolean> method){
        writeLock.lock();
        V output = null;
        if (method.apply(items.get(key))){
            output = items.remove(key);
        }
        writeLock.unlock();
        return output;
    }


    /**
     * Perform operation on a value if key is present, otherwise
     * set to defaultValue
     * @param key - Key for item to be updated by method
     * @param method - Method to update item, takes in value outputs new value
     * @param defaultValue - Value to set if key isn't present
     */
    public void apply(K key, Function<V, V> method, V defaultValue){
        writeLock.lock();

        if (!items.containsKey(key)){
            items.put(key, defaultValue);
        } else {
            items.put(key, method.apply(items.get(key)));
        }

        writeLock.unlock();
    }

    @Override
    public String toString() {
        return "SafeMap{" +
                "items=" + items +
                '}';
    }
}
