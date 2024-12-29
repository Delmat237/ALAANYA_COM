package audio;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.sound.sampled.SourceDataLine;

public class AudioReceiveThread extends Thread
{
    private final Socket socket;
    private final SourceDataLine speakers;

    public AudioReceiveThread(Socket socket, SourceDataLine speakers)
    {
        this.socket = socket;
        this.speakers = speakers;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            InputStream in = socket.getInputStream();

            while (!Thread.currentThread().isInterrupted() ) {
                int count = in.read(buffer, 0, buffer.length);
                if (count > 0)
                    speakers.write(buffer, 0, count);
                System.out.println("en ecoute");
            }
        } catch (IOException e) {
            System.out.println("Erreur dans la réception des packets audio : " + e.getMessage());
        }
    }

}