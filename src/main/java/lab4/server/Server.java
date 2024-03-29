package lab4.server;

import lab4.server.model.Task;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

public class Server {
    private final ServerSocket serverSocket;
    private volatile boolean isWorking = true;
    private final ConcurrentHashMap<Long, Task> map;
    private final ExecutorService taskExecutor;

    static final long TIME_TO_WORK = TimeUnit.MINUTES.toMillis(40);
    static final int TIMEOUT = (int) (TIME_TO_WORK / 2);

    public Server(ServerSocket serverSocket) throws SocketException {
        this.serverSocket = serverSocket;
        serverSocket.setSoTimeout(TIMEOUT);
        map = new ConcurrentHashMap<>();
        taskExecutor = Executors.newSingleThreadExecutor();
    }

    private Thread workThread;

    private Thread mainThread;

    public void start() throws InterruptedException {
        workThread = new Thread(this::work);
        mainThread = Thread.currentThread();
        workThread.start();
        try {
            Thread.sleep(TIME_TO_WORK);
        } catch (InterruptedException e) {
            if (isWorking) {
                isWorking = false;
                throw new RuntimeException(e);
            }
            System.out.println("The server was killed while sleeping...");
        }
        isWorking = false;
        taskExecutor.close();//also uses shutdown
        System.out.println("taskExecutor.shutdown()");
    }


    private void work() {
        try (var clientHandlerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3)) {
            while (isWorking || !clientHandlerExecutor.getQueue().isEmpty() ||
                    clientHandlerExecutor.getActiveCount() > 0 || !map.isEmpty()) {
                Socket accept = serverSocket.accept();
                ClientHandler handler = new ClientHandler(accept, taskExecutor, map, this::killServer);
                clientHandlerExecutor.submit(()->{
                    handler.run();
                    try {
                        accept.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout");
        } catch (IOException e) {
            if (isWorking) {
                isWorking = false;
                throw new RuntimeException(e);
            }
        }
    }

    private void killServer() {//отже клієнти вже під'єднуватися не будуть
        this.isWorking = false;
        System.out.println("Stop Server by Killer");
        workThread.interrupt();
        mainThread.interrupt();
    }
}
