package DocuStore;

import DocuStore.api.Connection;
import DocuStore.data.Record;
import DocuStore.data.RecordRequest;
import DocuStore.db.Server;
import DocuStore.db.SocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

public class App {
    private static final BlockingQueue<SocketHandler> handlerQueue = new LinkedBlockingQueue<>();

    public static void printBytes(String prefix, byte[] data){
//        return;
        StringBuilder dataBuilder = new StringBuilder(prefix);
        for (byte b : data) {
            dataBuilder.append(Integer.toHexString(0xff & b)).append(" ");
        }
        System.out.println(dataBuilder.toString());
    }

    static private void runTest(int thread, int seconds){
        Thread t = new Thread(() -> {
            String id = "" + thread + "_test";
            Connection connection = Connection.getConnection("test", "test");
            RecordRequest recordRequest;

            Record record;// = connection.fetch(recordRequest);
            try {
                Record rec = new Record(id, "test", "initial_value_getbytes".getBytes());
                HashMap<String, Object> map = new HashMap<>();
                map.put("Test", "value");
                map.put("thread", "" + thread);
                map.put("iteration", 0);
                rec.setData(map);
                System.out.println("After set data:" + rec.getObjectFromData());

                printBytes("in runTest: ", rec.getData());

                connection.store(rec);

                Thread.sleep(1000);
            } catch (Record.InvalidRecordException | InterruptedException e) {
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
                HashMap<String, Object> map = (HashMap<String, Object>) record.getObjectFromData();
                int iter = (int) map.get("iteration");
                iter++;
                map.put("iteration", iter);
                map.put("iteration_sq", Math.sqrt(iter*1.0));
                // Set the data to a new value
                if (!record.setData(map)){
//                    System.out.println("Main Updated record data");
//                } else {
                    System.out.println("Failed to update record data");
                }
                System.out.println("Main Test Thread " + thread + " about to send store request from fetch");
                connection.store(record);
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while(start.until(LocalDateTime.now(), ChronoUnit.SECONDS) <= seconds);
        });
        t.start();
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
        int duration = 5;

        for (int i = 0; i<args.length; i++){
            if (args[i].equals("-n") || args[i].equals("--numThreads")){
                numThreads = getNextArgInt(i, args, 4);
            } else if (args[i].equals("-t")){
                testThreads = getNextArgInt(i, args, 2);
            } else if (args[i].equals("-s")) {
                duration = getNextArgInt(i, args, 5);
            }
        }

        try {

            if (shouldTest){
                System.out.println("Main Testing");
                for (int i = 0; i<testThreads; i++) {
                    runTest(i, duration);
                }
            }

            System.out.println("Main Processing Threads: " + numThreads);

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
            Server server = Server.getServer(numThreads);
            if (server != null){
                server.start(true);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
