package DocuStore;

import DocuStore.api.Connection;
import DocuStore.data.Record;
import DocuStore.data.RecordRequest;
import DocuStore.db.SocketHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class App {
    private static BlockingQueue<SocketHandler> handlerQueue = new LinkedBlockingQueue<>();

    static public void execute(int thread){
        Thread t = new Thread(() -> {
            int i = 0;
            String id = "" + thread + "_test";
            Connection connection = Connection.getConnection("test", "test");
            RecordRequest<String> recordRequest = new RecordRequest<>("test", "test", "test", id);

            if (connection.fetch(recordRequest) == null){
                Record<String> rec = new Record<>(id, "test");
                rec.put("iter", 0);
                connection.store(rec);
            }


            while(true){
//                    Thread.sleep(200);
                recordRequest = new RecordRequest<>("test", "test", "test", id);

                Record<String> record = (Record<String>) connection.fetch(recordRequest);

                if (record == null){
                    continue;
                }
                System.out.println("Thread (" + thread + ") Request: " + record);
                record.put("iter", ((Integer) record.get("iter"))+1);
                record.put("iteration", "" + thread + "_" + record.get("iter"));
                connection.store(record);

            }
        });
        t.start();
    }

    static public void startNewProcessThread(int thread){
        Thread procThread = new Thread(() -> {
            SocketHandler handler = null;
            while (true){
                try {
                    handler = handlerQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (handler == null){
                        continue;
                    }
                    System.out.println("Thread (" + thread + ") Request Handled");
                    handler.run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        procThread.start();
    }


    public static void main(String[] args){
        ServerSocket serverSocket = null;
        int numThreads = 4;
        try {
            serverSocket = new ServerSocket(Record.PORT);
            for (int i = 0; i<10; i++){
                execute(i);
            }

            for (int i = 0; i<numThreads; i++){
                startNewProcessThread(i);
            }

            while(true){
                // wait for a request
                Socket socket = serverSocket.accept();
                SocketHandler sh = new SocketHandler(socket, 2000);
                try {
                    handlerQueue.put(sh);
//                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }



        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if (serverSocket != null){
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
