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

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface CallListener {
        void onCallReceived(String fromUser, String ip);
        void onCallAccepted(String ip);
        void onCallDeclined(String ip);
    }

    public void sendCallRequest(String remoteIP, String username) {
        try (Socket socket = new Socket(remoteIP, MainController.SIGNAL_PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println("CALL_REQUEST:" + username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCallResponse(String remoteIP, boolean accepted) {
        try (Socket socket = new Socket(remoteIP, MainController.SIGNAL_PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println(accepted ? "CALL_ACCEPTED" : "CALL_DECLINED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void listenForCallRequests(CallListener listener) {
        executor.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(MainController.SIGNAL_PORT)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket, listener);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleClient(Socket clientSocket, CallListener listener) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line = reader.readLine();
            if (line != null) {
                switch (line) {
                    case "CALL_ACCEPTED" -> listener.onCallAccepted(clientSocket.getInetAddress().getHostAddress());
                    case "CALL_DECLINED" -> listener.onCallDeclined(clientSocket.getInetAddress().getHostAddress());
                    default -> {
                        if (line.startsWith("CALL_REQUEST:")) {
                            String fromUser = line.substring("CALL_REQUEST:".length());
                            listener.onCallReceived(fromUser, clientSocket.getInetAddress().getHostAddress());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
