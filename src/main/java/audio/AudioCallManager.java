package audio;

public class AudioCallManager {
    private static final AudioSetup setup = new AudioSetup();
    private static AudioSender sender;
    private static AudioReceiver receiver;
    private static boolean isMuted = false;
    private static boolean isSpeakerOn = false;

    public static void startSending(String ip, int port) {
        try {
            if (sender == null || !sender.isSending()) {
                sender = new AudioSender(ip, port, setup);
                sender.start();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de l'envoi audio: " + e.getMessage());
        }
    }

    public static void startReceiving(int port) {
        try {
            if (receiver == null || !receiver.isReceiving()) {
                receiver = new AudioReceiver(port, setup);
                receiver.start();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de la réception audio: " + e.getMessage());
        }
    }

    public static void stopAll() {
        try {
            if (sender != null) {
                sender.stopSending();
                sender = null;
            }
            
            if (receiver != null) {
                receiver.stopReceiving();
                receiver = null;
            }
            
            setup.closeMicrophone();
            setup.closeSpeakers();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'arrêt des flux audio: " + e.getMessage());
        }
    }

    public static void setMute(boolean muted) {
        isMuted = muted;
        if (setup != null) {
            if (muted) {
                setup.muteMicrophone();
            } else {
                setup.unmuteMicrophone();
            }
        }
    }

    public static void setSpeakerMode(boolean speakerOn) {
        isSpeakerOn = speakerOn;
        if (setup != null) {
            setup.setSpeakerMode(speakerOn);
        }
    }

    public static boolean isMuted() {
        return isMuted;
    }

    public static boolean isSpeakerOn() {
        return isSpeakerOn;
    }

    public static boolean isRunning() {
        return (sender != null && sender.isSending()) || 
               (receiver != null && receiver.isReceiving());
    }
}