package DocuStore.api;

import DocuStore.App;
import DocuStore.data.Record;
import DocuStore.data.RecordRequest;
import DocuStore.db.InputStreamHelper;
import DocuStore.db.SocketHandler;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
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

    public void storeInBackground(Record record){
        new Thread(() -> instance.store(record)).run();
    }

    public void store(Record record){
        if (record == null){
            System.out.println("Conn ---------------- NO NULL RECORD!!!! -----------------------");
            return;
        }
        lock.lock();
        Socket s = null;
        try {
            s = new Socket(Record.HOST, Record.PORT);

            App.printBytes("in Connection.store: ", record.getData());

            s.getOutputStream().write(record.getBytes());
            s.getOutputStream().flush();
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

    public void fetchThen(RecordRequest recordRequest, Consumer<Record>then){
        then.accept(instance.fetch(recordRequest));
    }

    public Record fetch(RecordRequest recordRequest){
        Record output = null;
        try(Socket s = new Socket(Record.HOST, Record.PORT)) {
            s.setSoTimeout(2000);
            byte[] bytes = recordRequest.getBytes();

            App.printBytes("in Connection.fetch: ", bytes);

            s.getOutputStream().write(bytes);
            s.getOutputStream().flush();

            // The server only returns the bytes of the request
            byte[] inputData = s.getInputStream().readAllBytes();

            s.close();
            output = Record.getRecord(recordRequest.getId(), recordRequest.getPath(), inputData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }


}
