package DocuStore.db;

import DocuStore.data.Record;
import DocuStore.data.RecordRequest;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

public class SocketHandler implements Runnable {
    private final int timeout;
    private Socket socket;
    private LocalDateTime runTime;
    public boolean isClosed = false;

    public SocketHandler(Socket socket, int timeoutMillis){
        this.socket = socket;
        this.timeout = timeoutMillis;
    }

    @Override
    public void run() {
        runTime = LocalDateTime.now();
        try(InputStream inputStream = socket.getInputStream()){
            socket.setSoTimeout(timeout);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            Object obj =  objectInputStream.readObject();
            if (obj instanceof Record<?>){
                // Dast object to Record
                Record<?> record = (Record<?>) obj;
//                System.out.println("Record: " + record.toString());
                // Save the record
                FileManager.store(record);
            } else if (obj instanceof RecordRequest<?>){
                RecordRequest<?> recordRequest = (RecordRequest<?>) obj;
//                System.out.println("Record Request: " + recordRequest.toString());
                Record<?> record = FileManager.fetch( (RecordRequest<?>) obj);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStream.writeObject(record);
//                System.out.println("Fetched: " + String.valueOf(record));
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally{
            if (!socket.isClosed()){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        isClosed = true;
    }
}
