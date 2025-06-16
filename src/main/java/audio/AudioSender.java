package audio;

import javax.sound.sampled.TargetDataLine;
import java.io.OutputStream;
import java.net.Socket;

public class AudioSender extends Thread {
    private final String ip;
    private final int port;
    private final AudioSetup audioSetup;
    private volatile boolean running = true;


    public AudioSender(String ip, int port, AudioSetup audioSetup) {
        this.ip = ip;
        this.port = port;
        this.audioSetup = audioSetup;
    }

    @Override
    public void run() {
        //demande la connection à un server audio
        if (running) {
            try (Socket socket = new Socket(ip, port);
                 OutputStream out = socket.getOutputStream()) {

                // Ouvre le micro
                audioSetup.openMicrophone();
                TargetDataLine mic = audioSetup.getMicrophone();

                byte[] buffer = new byte[4096];
                while (!interrupted()) {
                    int count = mic.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        out.write(buffer, 0, count);
                    }
                }
            } catch (Exception e) {
              
               System.err.println("Error in AudioSender: " + e.getMessage());
            } catch (Error e) {
              
                System.err.println("Error in AudioSender: " + e.getMessage());

            } finally {
                audioSetup.closeMicrophone();
            }
        }
    }


    public boolean isSending() {
        return running;
    }
    public void setSending(boolean sending) {
        this.running = sending;
    }
    /**
     * Stop sending audio data.
     */
    public void stopSending() {
        running = false;
    }

}
