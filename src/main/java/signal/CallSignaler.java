package signal;

import controller.MainController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallSignaler {

    private static CallSignaler instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ServerSocket serverSocket;
    private volatile boolean listening = false;

    private CallSignaler() {}

    public interface CallListener {
        void onCallReceived(String fromUser, String ip, String type);
        void onCallAccepted(String ip, String type);
        void onCallDeclined(String ip);
    }

    public static synchronized CallSignaler getInstance() {
        if (instance == null) {
            instance = new CallSignaler();
        }
        return instance;

    }

    public static void stopInstance() {
        System.out.println("Instance courant "+instance);
        if (instance != null) {
            instance.stopListening();
            instance = null;
            System.out.println("Instance de CallSignaler arrêtée."+instance);
        }
    }


    public void sendCallRequest(String remoteIP, String username, String type) {
        try (Socket socket = new Socket(remoteIP, MainController.SIGNAL_PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println("CALL_REQUEST:" + username + ":" + type);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendCallResponse(String remoteIP, boolean accepted, String callType) {
        try (Socket socket = new Socket(remoteIP, MainController.SIGNAL_PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println(accepted
                    ? "CALL_ACCEPTED:" + callType
                    : "CALL_DECLINED:" + callType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public synchronized void listenForCallRequests(CallListener listener) {
        if (listening) return; // déjà en écoute

        listening = true;
        executor.submit(() -> {
            try {
                serverSocket = new ServerSocket(MainController.SIGNAL_PORT);
                while (listening) {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket, listener);
                }
            } catch (IOException e) {
                if (listening) {
                    e.printStackTrace();
                }
            } finally {
                closeServerSocket();
            }
        });
    }
    public synchronized void stopListening() {
        listening = false;
        closeServerSocket();
        executor.shutdownNow();
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClient(Socket clientSocket, CallListener listener) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line = reader.readLine();
            if (line != null) {
                String ip = clientSocket.getInetAddress().getHostAddress();

                if (line.startsWith("CALL_REQUEST:")) {
                    // Format : CALL_REQUEST:username:type
                    String[] parts = line.split(":", 3);
                    if (parts.length == 3) {
                        String fromUser = parts[1];
                        String callType = parts[2];
                        listener.onCallReceived(fromUser, ip, callType);
                    }
                } else if (line.startsWith("CALL_ACCEPTED:")) {
                    String callType = line.substring("CALL_ACCEPTED:".length());
                    listener.onCallAccepted(ip, callType);
                } else if (line.startsWith("CALL_DECLINED:")) {
                    listener.onCallDeclined(ip);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
