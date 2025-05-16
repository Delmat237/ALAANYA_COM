package video;

import org.bytedeco.javacv.*;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class CameraService extends Thread {
    private final OpenCVFrameGrabber grabber;
    private final Consumer<BufferedImage> frameConsumer;
    private final Java2DFrameConverter converter;
    private volatile boolean running = true;
    private volatile boolean started = false;

    public CameraService(Consumer<BufferedImage> frameConsumer) {
        this.frameConsumer = frameConsumer;
        this.grabber = new OpenCVFrameGrabber(0);
        this.converter = new Java2DFrameConverter();
    }

    @Override
    public void run() {
        try {
            grabber.setImageWidth(1920);
            grabber.setImageHeight(1080);
            grabber.start();
            
            started = true;
            while (running) {
                Frame frame = grabber.grab();
                if (frame != null) {
                    BufferedImage img = converter.convert(frame);
                    if (img != null) {
                        frameConsumer.accept(img);
                    }
                }
                Thread.sleep(33); // ~30 FPS
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            started = false;
        }
    }
    public boolean isStarted() {
        return started;
    }

    public void stopCapture() {
        running = false;
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public BufferedImage grabCurrentFrame() throws Exception {
        if (!started) {
            throw new IllegalStateException("Grabber not started yet");
        }
        Frame frame = grabber.grab();
        if (frame == null) {
            return null;
        }
        return converter.convert(frame);
    }
}
