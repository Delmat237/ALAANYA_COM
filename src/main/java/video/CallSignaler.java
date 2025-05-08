package video;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class CallSignaler {

    public interface CallListener {
        void onCallReceived(String fromUser, String ip);
        void onCallAccepted(String ip);
        void onCallDeclined(String ip);
    }

    public void sendCallRequest(String remoteIP, String username) {
        try (Socket socket = new Socket(remoteIP, 6000);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println("CALL_REQUEST:" + username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCallResponse(String remoteIP, boolean accepted) {
        try (Socket socket = new Socket(remoteIP, 6000);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println(accepted ? "CALL_ACCEPTED" : "CALL_DECLINED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listenForCallRequests(CallListener listener) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(6000)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String line = reader.readLine();
                    if (line != null) {
                        if (line.startsWith("CALL_REQUEST:")) {
                            String fromUser = line.substring("CALL_REQUEST:".length());
                            listener.onCallReceived(fromUser, clientSocket.getInetAddress().getHostAddress());
                        } else if (line.equals("CALL_ACCEPTED")) {
                            listener.onCallAccepted(clientSocket.getInetAddress().getHostAddress());
                        } else if (line.equals("CALL_DECLINED")) {
                            listener.onCallDeclined(clientSocket.getInetAddress().getHostAddress());
                        }
                    }
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
