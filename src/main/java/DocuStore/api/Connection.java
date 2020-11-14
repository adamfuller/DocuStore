package DocuStore.api;

import DocuStore.data.Record;
import DocuStore.data.RecordRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class Connection {
    private static Connection instance;
    private String user;
    private String key;
    final private static ReentrantLock lock = new ReentrantLock();

    private Connection() {

    }

    private Connection(String user, String key){
        this.user = user;
        this.key = key;
    }

    public static Connection getConnection(String user, String key){
        if (instance == null){
            instance = new Connection(user, key);
        }
        return instance;
    }

    public void storeInBackground(Record<?> record){
        new Thread(() -> instance.store(record)).run();
    }

    public void store(Record<?> record){
        lock.lock();
        Socket s = null;
        try {
            s = new Socket(Record.HOST, Record.PORT);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(s.getOutputStream());
            objectOutputStream.writeObject(record);
            objectOutputStream.flush();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if (s != null){
                if (!s.isClosed()){
                    try {
                        s.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            lock.unlock();
        }
    }

    public void fetchThen(RecordRequest<?> recordRequest, Consumer<Record<?>>then){
        then.accept(instance.fetch(recordRequest));
    }

    public Record<?> fetch(RecordRequest<?> recordRequest){

        try(Socket s = new Socket(Record.HOST, Record.PORT);) {

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(s.getOutputStream());
            objectOutputStream.writeObject(recordRequest);
            ObjectInputStream objectInputStream = new ObjectInputStream(s.getInputStream());
            Object o = objectInputStream.readObject();
            objectInputStream.close();
            s.close();
            if (o instanceof Record<?>){
                return (Record<?>) o;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


}
