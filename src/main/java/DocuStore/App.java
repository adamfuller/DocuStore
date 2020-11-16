package DocuStore;

import DocuStore.api.Connection;
import DocuStore.data.Record;
import DocuStore.data.RecordRequest;
import DocuStore.db.SocketHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class App {
    private static final BlockingQueue<SocketHandler> handlerQueue = new LinkedBlockingQueue<>();


    static public void runTest(int thread, int seconds){
        Thread t = new Thread(() -> {
            System.out.println("Main In test thread");
            int i = 0;
            String id = "" + thread + "_test";
            Connection connection = Connection.getConnection("test", "test");
            RecordRequest recordRequest = new RecordRequest("test", "test", "test", id);

            System.out.println("Main Test Thread " + thread + " about to send fetch request (init)");
            Record record = connection.fetch(recordRequest);
            if (record == null){
                Record rec = null;
                try{
                    rec = new Record(id, "test", "initial_value".getBytes());
                    rec.setData("initial_value");
                } catch (Exception | Record.InvalidRecordException e){
                    e.printStackTrace();
                }
                if (rec != null){
                    System.out.println("Main Test Thread " + thread + " about to send store request (init)");
                    connection.store(rec);
                } else {
                    System.out.println("Main Test Thread " + thread + " Failed to create record");
                }
            } else {
                System.out.println("Main Test Thread " + thread + " Record fetched");
            }

            final LocalDateTime start = LocalDateTime.now();
            do{
                recordRequest = new RecordRequest("test", "test", "test", id);
                System.out.println("Main Test Thread " + thread + " about to send fetch request");
                record = connection.fetch(recordRequest);
                System.out.println("Record received : " + record);
                if (record == null){
                    continue;
                }
                System.out.println("Main Fetched record with data : " + record.getObjectFromData());
                HashMap<String, String> map = new HashMap<>();
                map.put("Test", "value");
                // Set the data to a new value
                if (record.setData(map)){
                    System.out.println("Main Updated record data");
                } else {
                    System.out.println("Failed to update record data");
                }
                System.out.println("Main Test Thread " + thread + " about to send store request from fetch");
                connection.store(record);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while(start.until(LocalDateTime.now(), ChronoUnit.SECONDS) <= seconds);
//            System.out.println("Main Done testing on Thread " + thread);
        });
        t.start();
    }

    static public void startNewProcessThread(int thread){

        Thread procThread = new Thread(() -> {
            SocketHandler handler = null;
            System.out.println("Main In Processing Thread");
            while (true){
                try {

                    handler = handlerQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (handler == null){
                        continue;
                    }
                    if (!handler.isRunning()){
                        System.out.println("Main Proc Thread " + thread + " About to handle request");
                        handler.run();
//                        System.out.println("Main Thread (" + thread + ") Request Handled");
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        procThread.start();
    }

    private static Integer getNextArgInt(int index, String[] args){
        return getNextArgInt(index, args, null);
    }

    private static Integer getNextArgInt(int index, String[] args, Integer defaultValue){
        if (index+1<args.length){
            try{
                return Integer.parseInt(args[index+1]);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return defaultValue;
    }

    public static void main(String[] args){
        Set<String> inputArgs = new HashSet<>(Arrays.asList(args));

        boolean shouldTest = inputArgs.contains("-t");
        int numThreads = 4;
        int testThreads = 2;

        for (int i = 0; i<args.length; i++){
            if (args[i].equals("-n") || args[i].equals("--numThreads")){
                numThreads = getNextArgInt(i, args, 4);
            } else if (args[i].equals("-t")){
                testThreads = getNextArgInt(i, args, 2);
            }
        }


        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(Record.PORT);

            if (shouldTest){
                System.out.println("Main Testing");
                for (int i = 0; i<testThreads; i++) {
                    runTest(i, 5);
                }
            }

            System.out.println("Main Processing Threads: " + numThreads);
            for (int i = 0; i<numThreads; i++){
                startNewProcessThread(i);
            }

            Scanner console = new Scanner(System.in);
            BlockingQueue<String> userInput = new LinkedBlockingQueue<>();


            Thread inputHandler = new Thread(() ->{
                while(true){
                    if (console.hasNext()){
                        String input = console.next();
//                        System.out.println("Main Read user input: " + input);
                        if (input.strip().equals("t")){
                            runTest(0, -1);
                        }
                    };
                }
            });
            inputHandler.start();

            // Start processing requests
            while(true){
                // wait for a request
                Socket socket = serverSocket.accept();
                if (socket == null){
                    continue;
                }
                SocketHandler sh = new SocketHandler(socket, 5000);

                try {
                    handlerQueue.put(sh);
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
