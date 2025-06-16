package audio;

import javax.sound.sampled.*;

public class AudioSetup {
    private final AudioFormat format;
    private TargetDataLine microphone;
    private SourceDataLine speakers;

    public AudioSetup() {
        // 44.1 kHz, 16 bits, mono, signed, little endian
        this.format = new AudioFormat(44100.0f, 16, 1, true, false);
    }

    // --- Microphone ---

    public void openMicrophone() throws LineUnavailableException {
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
        microphone.open(format);
        microphone.start();
    }

    public void closeMicrophone() {
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }


    }

    public TargetDataLine getMicrophone() {
        return microphone;
    }

    // --- Speakers ---

    public void openSpeakers() throws LineUnavailableException {
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
        speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speakers.open(format);
        speakers.start();
    }

    public void closeSpeakers() {
        if (speakers != null) {
            speakers.drain();
            speakers.stop();
            speakers.close();
        }
    }

    public SourceDataLine getSpeakers() {
        return speakers;
    }

    public AudioFormat getFormat() {
        return format;
    }


    public void muteMicrophone() {
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
        }
        // Implémentation pour désactiver le micro
    }
    
    public void unmuteMicrophone() {
        if (microphone != null && !microphone.isOpen()) {
            try {
                microphone.start();
            } catch (Exception e) {
                System.err.println("Erreur lors de la réactivation du micro: " + e.getMessage());
            }
        }
        // Implémentation pour réactiver le micro
    }
    public boolean isMicrophoneOpen() {
        return microphone != null && microphone.isOpen();
    }
    public boolean isSpeakersOpen() {
        return speakers != null && speakers.isOpen();
    }
    
    public void setSpeakerMode(boolean enabled) {
        if (speakers != null) {
            if (enabled) {
                if (!speakers.isOpen()) {
                    try {
                        speakers.open(format);
                        speakers.start();
                    } catch (LineUnavailableException e) {
                        System.err.println("Erreur lors de l'ouverture des haut-parleurs: " + e.getMessage());
                    }
                }
            } else {
                speakers.stop();
                speakers.close();
            }
        }
    }

}
