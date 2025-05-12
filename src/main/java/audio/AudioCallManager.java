package audio;

public class AudioCallManager {
    private final AudioSetup setup = new AudioSetup();
    private AudioSender sender;
    private AudioReceiver receiver;

    public static void startSending(String ip, int port) {
        sender = new AudioSender(ip, port, setup);
        sender.start();
    }

    public void startReceiving(int port) {
        receiver = new AudioReceiver(port, setup);
        receiver.start();
    }

    public void stopAll() {
        if (sender != null) sender.stopSending();
        if (receiver != null) receiver.stopReceiving();
        setup.closeMicrophone();
        setup.closeSpeakers();
    }
}
