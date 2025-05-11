package utils;

import javax.sound.sampled.*;
import java.io.InputStream;
import java.io.BufferedInputStream;

public class SoundPlayer {
    public static void playSound(String resourcePath) {
        try {
            InputStream audioSrc = SoundPlayer.class.getResourceAsStream(resourcePath);
            if (audioSrc == null) {
                System.err.println("Audio resource not found: " + resourcePath);
                return;
            }

            BufferedInputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);

            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
