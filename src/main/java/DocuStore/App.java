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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class App {
    private static BlockingQueue<SocketHandler> handlerQueue = new LinkedBlockingQueue<>();

    static public void runTest(int thread, int seconds){
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

            final LocalDateTime start = LocalDateTime.now();
            do{
//                    Thread.sleep(200);
                recordRequest = new RecordRequest<>("test", "test", "test", id);

                Record<String> record = (Record<String>) connection.fetch(recordRequest);

                if (record == null){
                    continue;
                }
                System.out.println("Thread (" + thread + ") Request Received: " + record);
                record.put("iter", ((Integer) record.get("iter"))+1);
                record.put("iteration", "" + thread + "_" + record.get("iter"));
                connection.store(record);

            }while(start.until(LocalDateTime.now(), ChronoUnit.SECONDS) <= seconds);
            System.out.println("Done testing on Thread " + thread);
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
                    if (!handler.isRunning()){
                        handler.run();
                        System.out.println("Thread (" + thread + ") Request Handled");
                    }
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

        for (int i = 0; i<args.length; i++){
            if (args[i].equals("-n") || args[i].equals("--numThreads")){
                numThreads = getNextArgInt(i, args, 4);
            }
        }
        System.out.println("Processing Threads: " + numThreads);

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(Record.PORT);

            if (shouldTest){
                System.out.println("Testing");
                for (int i = 0; i<10; i++) {
                    runTest(i, 1);
                }
            }

            for (int i = 0; i<numThreads; i++){
                startNewProcessThread(i);
            }

            Scanner console = new Scanner(System.in);
            BlockingQueue<String> userInput = new LinkedBlockingQueue<>();


            Thread inputHandler = new Thread(() ->{
                while(true){
                    if (console.hasNext()){
                        String input = console.next();
                        System.out.println("Read user input: " + input);
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
                SocketHandler sh = new SocketHandler(socket, 2000);

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
