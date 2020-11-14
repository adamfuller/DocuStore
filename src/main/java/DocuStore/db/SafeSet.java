package DocuStore.db;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

class SafeSet<E> {
    final private Set<E> items;
    private Map<E, ArrayList<Consumer<E>>> onRemoveCallbacks = new HashMap<>();
    ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    Lock readLock = readWriteLock.readLock();
    Lock writeLock = readWriteLock.writeLock();

    public SafeSet(){
        items = new HashSet<>();
    }


    public boolean contains(E item){
        readLock.lock();
        boolean val = items.contains(item);
        readLock.unlock();
        return val;
    }

    /**
     * Add the item to the Set
     * @param item
     * @return - True if the item wasn't already present, false otherwise
     */
    public boolean add(E item){
        writeLock.lock();
        boolean output = items.add(item);
        writeLock.unlock();
        return output;
    }

    public boolean remove(E item){
        writeLock.lock();
        boolean output = items.remove(item);
        if (onRemoveCallbacks.containsKey(item)){
            for (Consumer<E> consumer: onRemoveCallbacks.get(item)){
                consumer.accept(item);
            }
        }
        writeLock.unlock();
        return output;
    }

    public void onRemoveOnce(E item, Consumer<E> consumer){
        if (this.contains(item)){
            writeLock.lock();
            if (!onRemoveCallbacks.containsKey(item)){
                onRemoveCallbacks.put(item, new ArrayList<>());
            }
            onRemoveCallbacks.get(item).add(consumer);
            // Remove the callback after it's triggered, will already be write locked
            onRemoveCallbacks.get(item).add((e)->{
                onRemoveCallbacks.get(item).remove(consumer);
            });
            writeLock.unlock();
        }
    }


}
