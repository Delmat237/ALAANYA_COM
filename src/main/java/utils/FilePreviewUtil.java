package utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.FileInputStream;

public class FilePreviewUtil {

    public static final double MAX_WIDTH = 200;
    private static final double MAX_HEIGHT = 200;

    public static Node createFilePreview(String fileName, String extension) {
        try {
            File file = new File(fileName);
            if (!file.exists()) return null;

            return switch (extension.toLowerCase()) {
                case "jpg", "jpeg", "png", "gif", "bmp" -> {
                    Image img = new Image(file.toURI().toString(), MAX_WIDTH, MAX_HEIGHT, true, true);
                    ImageView imageView = new ImageView(img);
                    imageView.setPreserveRatio(true);
                    yield imageView;
                }

                case "txt", "md", "rtf", "xml" -> {
                    StringBuilder previewText = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        int lines = 0;
                        while ((line = br.readLine()) != null && lines++ < 5)
                            previewText.append(line).append("\n");
                    }
                    Text preview = new Text(previewText.toString());
                    preview.setWrappingWidth(MAX_WIDTH);
                    preview.setStyle("-fx-font-size: 12px; -fx-fill: #444;");
                    yield preview;
                }

                case "mp3", "wav", "aac", "ogg", "m4a" -> {
                    Media media = new Media(file.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);

                    FontIcon playIcon = new FontIcon(FontAwesomeSolid.PLAY);
                    FontIcon pauseIcon = new FontIcon(FontAwesomeSolid.PAUSE);
                    FontIcon muteIcon = new FontIcon(FontAwesomeSolid.VOLUME_MUTE);
                    FontIcon unmuteIcon = new FontIcon(FontAwesomeSolid.VOLUME_UP);

                    Button playPauseButton = new Button("", playIcon);
                    Button muteButton = new Button("", muteIcon);

                    playPauseButton.setStyle("-fx-background-color: #eee;");
                    muteButton.setStyle("-fx-background-color: #eee;");

                    Label timeLabel = new Label("00:00 / 00:00");
                    timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #444;");

                    Slider progressSlider = new Slider();
                    progressSlider.setMin(0);
                    progressSlider.setMax(100);
                    progressSlider.setPrefWidth(MAX_WIDTH - 40);
                    progressSlider.setDisable(true);

                    playPauseButton.setOnAction(e -> {
                        MediaPlayer.Status status = mediaPlayer.getStatus();
                        if (status == MediaPlayer.Status.PLAYING) {
                            mediaPlayer.pause();
                            playPauseButton.setGraphic(playIcon);
                        } else {
                            mediaPlayer.play();
                            playPauseButton.setGraphic(pauseIcon);
                        }
                    });

                    muteButton.setOnAction(e -> {
                        boolean isMuted = mediaPlayer.isMute();
                        mediaPlayer.setMute(!isMuted);
                        muteButton.setGraphic(isMuted ? muteIcon : unmuteIcon);
                    });

                    mediaPlayer.setOnReady(() -> {
                        progressSlider.setDisable(false);
                        Duration total = mediaPlayer.getTotalDuration();
                        progressSlider.setMax(total.toSeconds());
                        updateTimeLabel(timeLabel, Duration.ZERO, total);
                    });

                    mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                        if (!progressSlider.isValueChanging()) {
                            progressSlider.setValue(newTime.toSeconds());
                            updateTimeLabel(timeLabel, newTime, mediaPlayer.getTotalDuration());
                        }
                    });

                    progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                        if (!isChanging) {
                            mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
                        }
                    });

                    HBox controlsRow = new HBox(10, playPauseButton, muteButton, progressSlider);
                    controlsRow.setAlignment(Pos.CENTER);

                    VBox controls = new VBox(5, controlsRow, timeLabel);
                    controls.setAlignment(Pos.CENTER);
                    controls.setPadding(new Insets(5));

                    mediaPlayer.setMute(true);
                    mediaPlayer.setAutoPlay(false);

                    yield controls;
                }

                case "mp4", "m4v", "mov", "avi" -> {
                    Media media = new Media(file.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(media);
                    MediaView mediaView = new MediaView(mediaPlayer);
                    mediaView.setFitWidth(MAX_WIDTH);
                    mediaView.setFitHeight(MAX_HEIGHT);
                    mediaView.setPreserveRatio(true);

                    FontIcon playIcon = new FontIcon(FontAwesomeSolid.PLAY);
                    FontIcon pauseIcon = new FontIcon(FontAwesomeSolid.PAUSE);
                    FontIcon muteIcon = new FontIcon(FontAwesomeSolid.VOLUME_MUTE);
                    FontIcon unmuteIcon = new FontIcon(FontAwesomeSolid.VOLUME_UP);

                    Button playPauseButton = new Button("", playIcon);
                    Button muteButton = new Button("", muteIcon);

                    playPauseButton.setStyle("-fx-background-color: #eee;");
                    muteButton.setStyle("-fx-background-color: #eee;");

                    Label timeLabel = new Label("00:00 / 00:00");
                    timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #444;");

                    Slider progressSlider = new Slider();
                    progressSlider.setMin(0);
                    progressSlider.setMax(100);
                    progressSlider.setPrefWidth(MAX_WIDTH - 40);
                    progressSlider.setDisable(true);

                    playPauseButton.setOnAction(e -> {
                        MediaPlayer.Status status = mediaPlayer.getStatus();
                        if (status == MediaPlayer.Status.PLAYING) {
                            mediaPlayer.pause();
                            playPauseButton.setGraphic(playIcon);
                        } else {
                            mediaPlayer.play();
                            playPauseButton.setGraphic(pauseIcon);
                        }
                    });

                    muteButton.setOnAction(e -> {
                        boolean isMuted = mediaPlayer.isMute();
                        mediaPlayer.setMute(!isMuted);
                        muteButton.setGraphic(isMuted ? muteIcon : unmuteIcon);
                    });

                    mediaPlayer.setOnReady(() -> {
                        progressSlider.setDisable(false);
                        Duration total = mediaPlayer.getTotalDuration();
                        progressSlider.setMax(total.toSeconds());
                        updateTimeLabel(timeLabel, Duration.ZERO, total);
                    });

                    mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                        if (!progressSlider.isValueChanging()) {
                            progressSlider.setValue(newTime.toSeconds());
                            updateTimeLabel(timeLabel, newTime, mediaPlayer.getTotalDuration());
                        }
                    });

                    progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                        if (!isChanging) {
                            mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
                        }
                    });

                    HBox controlsRow = new HBox(10, playPauseButton, muteButton, progressSlider);
                    controlsRow.setAlignment(Pos.CENTER);

                    VBox controls = new VBox(5, mediaView, controlsRow, timeLabel);
                    controls.setAlignment(Pos.CENTER);
                    controls.setPadding(new Insets(5));

                    mediaPlayer.setMute(true);
                    mediaPlayer.setAutoPlay(false);

                    yield controls;
                }
                case "pdf" -> {
                    PDDocument document = PDDocument.load(file);
                    PDFRenderer pdfRenderer = new PDFRenderer(document);
                    BufferedImage image = pdfRenderer.renderImageWithDPI(0, 100); // première page à 100 DPI
                    document.close();

                    WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
                    ImageView imageView = new ImageView(fxImage);
                    imageView.setFitWidth(MAX_WIDTH);
                    imageView.setPreserveRatio(true);
                    yield imageView;
                }
                case "docx" -> {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        XWPFDocument doc = new XWPFDocument(fis);
                        StringBuilder text = new StringBuilder();
                        for (XWPFParagraph p : doc.getParagraphs()) {
                            text.append(p.getText()).append("\n");
                            if (text.length() > 500) break; // limiter l'aperçu
                        }
                        Text preview = new Text(text.toString());
                        preview.setWrappingWidth(MAX_WIDTH);
                        preview.setStyle("-fx-font-size: 12px; -fx-fill: #444;");
                        yield preview;
                    }
                }



                default -> null;
            };

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static String formatTime(Duration d) {
        int minutes = (int) d.toMinutes();
        int seconds = (int) d.toSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static void updateTimeLabel(Label label, Duration current, Duration total) {
        String currentStr = formatTime(current);
        String totalStr = formatTime(total);
        label.setText(currentStr + " / " + totalStr);
    }
}
