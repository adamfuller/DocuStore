package DocuStore.db;

import DocuStore.data.Record;
import DocuStore.data.RecordRequest;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

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

    @Override
    public void run() {
        // Only run once
        if (this.isRunning){
            System.out.println("SoHa Request handled multiple times");
            return;
        }
        isRunning = true;
        runTime = LocalDateTime.now();

        try {
            socket.setSoTimeout(timeout);
            Optional<byte[]> output = InputStreamHelper.process(socket.getInputStream());
            if (output.isPresent()){
                OutputStreamHelper.process(socket.getOutputStream(), output.get());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        isClosed = true;
    }
}
