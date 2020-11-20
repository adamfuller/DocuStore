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

import static java.lang.Thread.sleep;

public class App {
    private static final BlockingQueue<SocketHandler> handlerQueue = new LinkedBlockingQueue<>();

    public static void printBytes(String prefix, byte[] data){
        return;
//        StringBuilder dataBuilder = new StringBuilder(prefix);
//        for (byte b : data) {
//            dataBuilder.append(Integer.toHexString(0xff & b)).append(" ");
//        }
//        System.out.println(dataBuilder.toString());
    }

    static private void runTest(int thread, int seconds){
        Thread t = new Thread(() -> {
            String id = "" + thread + "_test";
            Connection connection = Connection.getConnection("test", "test");
            RecordRequest recordRequest;

            Record record;// = connection.fetch(recordRequest);
            try {
                Record rec = new Record(id, "test", "initial_value_getbytes".getBytes());
                rec.setData("initial_value");
                System.out.println("After set data:" + rec.getObjectFromData());

                printBytes("in runTest: ", rec.getData());

                connection.store(rec);

            } catch (Record.InvalidRecordException e) {
                e.printStackTrace();
            }

            final LocalDateTime start = LocalDateTime.now();
            do{
                recordRequest = new RecordRequest("test", "test", "test", id);
//                System.out.println("Main Test Thread " + thread + " about to send fetch request");
                record = connection.fetch(recordRequest);

                if (record == null){
                    continue;
                }

                printBytes("in runTest after fetch: ", record.getData());

                System.out.println("Main Fetched record with data : " + record.getObjectFromData());
//                HashMap<String, String> map = new HashMap<>();
//                map.put("Test", "value");
                // Set the data to a new value
                if (record.setData("second_value")){
                    System.out.println("Main Updated record data");
                } else {
                    System.out.println("Failed to update record data");
                }
                System.out.println("Main Test Thread " + thread + " about to send store request from fetch");
                connection.store(record);
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while(start.until(LocalDateTime.now(), ChronoUnit.SECONDS) <= seconds);
        });
        t.start();
    }

    static private void startNewProcessThread(){

        Thread procThread = new Thread(() -> {

            while (true){
                try {
                    SocketHandler handler = handlerQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (handler == null){
                        continue;
                    }
                    handler.run();
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        procThread.start();
    }

//    private static Integer getNextArgInt(int index, String[] args){
//        return getNextArgInt(index, args, null);
//    }

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
                startNewProcessThread();
            }

            Scanner console = new Scanner(System.in);

            Thread inputHandler = new Thread(() ->{
                while(true){
                    if (console.hasNext()){
                        String[] input = console.next().split(" ");
                        if (input.length == 0){
                            continue;
                        }

                        if (input[0].equals("t")){
                            if (input.length == 2){
                                int numTests = Integer.parseInt(input[1]);
                                for (int i = 1; i<numTests; i++){
                                    runTest(i, -1);
                                }
                            }
                            runTest(0, -1);
                        }
                    }
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
