package message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MessageWriteThread extends Thread {
    private final DataOutputStream out;

    public MessageWriteThread(Socket socket) throws IOException {
        out = new DataOutputStream(socket.getOutputStream());
    }

    public void send(String message) throws IOException {
        if (message != null){
            out.writeUTF(message);
        }
    }

    public void sendLong(long message) throws IOException {
        out.writeLong(message);
    }
}
