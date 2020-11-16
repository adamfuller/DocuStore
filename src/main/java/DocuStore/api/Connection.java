package DocuStore.api;

import DocuStore.data.Record;
import DocuStore.data.RecordRequest;
import DocuStore.db.SocketHandler;

import java.io.IOException;
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
            System.out.println("Conn About to send record: " + new String(record.getBytes()));
            s = new Socket(Record.HOST, Record.PORT);
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

        try(Socket s = new Socket(Record.HOST, Record.PORT)) {
            s.setSoTimeout(2000);
            byte[] bytes = recordRequest.getBytes();
            s.getOutputStream().write(bytes);
//            s.getOutputStream().flush();
            System.out.println("Conn Sent fetch for: " + new String(bytes));
            byte[] inputData = SocketHandler.readInputStream(s.getInputStream());
            System.out.println("Conn Fetch result: " + new String(inputData));
            s.close();
            if (inputData.length <= ":::".getBytes().length){
                return null;
            }
            return Record.fromBytes(inputData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
