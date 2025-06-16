package audio;

import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class AudioReceiver extends Thread {
    private final int port;
    private final AudioSetup audioSetup;
    private volatile boolean running = true;


    public AudioReceiver(int port, AudioSetup audioSetup) {
        this.port = port;
        this.audioSetup = audioSetup;
    }

    @Override
    public void run() {
        if (running){
            try (ServerSocket server = new ServerSocket(port);
                 Socket socket = server.accept();
                 InputStream in = socket.getInputStream()) {

                System.out.println("serveur des audio en attente de connexion");
                // Ouvre les enceintes
                audioSetup.openSpeakers();
                SourceDataLine speakers = audioSetup.getSpeakers();

                byte[] buffer = new byte[4096];
                int count;
                while ((count = in.read(buffer)) > 0) {

                    speakers.write(buffer, 0, count);
                }
            } catch (Exception e) {
                System.out.println("Appel déjà en cours ");
                e.printStackTrace();
            } finally {
                audioSetup.closeSpeakers();
            }
        }
    }
    public boolean isReceiving() {
        return running;
    }
    public void setReceiving(boolean receiving) {
        this.running = receiving;
    }
    /**
     * Stop receiving audio data.
     */
    public void stopReceiving() {
        running = false;
    }

}
