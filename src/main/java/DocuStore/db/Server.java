package DocuStore.db;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class Server {
    final public static String HOST = "localhost";
    final public static int PORT = 8081;
    private static final BlockingQueue<SocketHandler> handlerQueue = new LinkedBlockingQueue<>();
    private static Server instance;
    private static int threadCount = 0;

    private Server(int threadCount){
        Server.threadCount = threadCount;
        for (int i = 0; i<threadCount; i++){
            startNewProcessThread();
        }
    }

    public static Server getServer(int threadCount){
        if (threadCount <= 0){
            return null;
        }
        if (instance == null){
            instance = new Server(threadCount);
        }
        return instance;
    }

    /**
     * Start listening on the server
     * @param block - block the current thread till the server finishes
     * @throws IOException - ServerSocket fails
     */
    public void start(boolean block) throws IOException {
        if (threadCount == 0){
            System.out.println("Not starting server since there are no threads, call Server.getServer");
            return;
        }
        if (!block){
            Thread t = new Thread(() -> {
                try {
                    this.start(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t.start();
            return;
        }

        try(ServerSocket serverSocket = new ServerSocket(PORT)){
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
        }
    }

    static private void startNewProcessThread(){

        Thread procThread = new Thread(() -> {
            while (true){
                try {
                    SocketHandler handler = handlerQueue.poll(1000, TimeUnit.MILLISECONDS);
                    if (handler == null){
                        sleep(100);
                        continue;
                    }
                    handler.run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        procThread.start();
    }
}
