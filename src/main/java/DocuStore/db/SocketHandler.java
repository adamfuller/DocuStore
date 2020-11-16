package DocuStore.db;

import DocuStore.data.Record;
import DocuStore.data.RecordRequest;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Arrays;

/*
    Pull from Socket util 5th ::
        Store in String
    Split String by ::
        USER::KEY::ID::PATH::DATA::
    Pass DATA to FileManager for writing to proper file
 */
public class SocketHandler implements Runnable {
    private final int timeout;
    private Socket socket;
    private LocalDateTime runTime;
    public boolean isClosed = false;
    private boolean isRunning = false;

    public SocketHandler(Socket socket, int timeoutMillis){
        this.socket = socket;
        this.timeout = timeoutMillis;
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    static public byte[] readInputStream(InputStream is) throws IOException {
        int count = 0;
        byte[] buffer = new byte[1064];
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try{
            while((count = is.read(buffer)) > 0){
                System.out.println("SoHa Read " + count + " bytes");
                output.write(buffer, 0, count);

                sb.append(new String(Arrays.copyOf(buffer, count)));
                if (sb.toString().contains(":::")){
                    break;
                }
            }
        } catch (IOException e){
            // Do nothing
        }
        return output.toByteArray();
    }

    @Override
    public void run() {
        // Only run once
        if (this.isRunning){
            System.out.println("SoHa Request handled multiple times");
            return;
        }
        isRunning = true;
        runTime = LocalDateTime.now();
        try(InputStream inputStream = socket.getInputStream()){
            System.out.println("SoHa Thread Got input stream");
//            socket.setSoTimeout(timeout);
            // Read all bytes
            byte[] wholeInput = readInputStream(inputStream);
            System.out.println("SoHa Read " + wholeInput.length + " bytes: " + new String(wholeInput));
            Record record = Record.fromBytes(wholeInput);
            RecordRequest recordRequest = RecordRequest.fromBytes(wholeInput);

            if (record != null){
                // Save the record
                FileManager.store(record);
            } else if (recordRequest != null) {
                record = FileManager.fetch(recordRequest);
                if (record != null){
                    socket.getOutputStream().write(record.getBytes());
                    System.out.println("SoHa Sending response: " + new String(record.getBytes()));
                } else {
                    System.out.println("SoHa failed to fetch " + recordRequest.getFullPath());
                    // Send end string
                    socket.getOutputStream().write(":::".getBytes());
                    socket.getOutputStream().flush();
                    System.out.println("SoHa Record " + recordRequest.toString() + " was not present");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if (!socket.isClosed()){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            isRunning = false;
        }
        isClosed = true;
    }
}
