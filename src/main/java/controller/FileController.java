package controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FileController {
    // Constants
    private static final double MAX_WIDTH = 250;
    private static final String SENT_BG_COLOR = "#26631b";
    private static final String RECEIVED_BG_COLOR = "#FFFFFF";
    private static final String SENT_HOVER_COLOR = "#c5e1b0";
    private static final String RECEIVED_HOVER_COLOR = "#f5f5f5";
    private static final String SENT_EXIT_COLOR = "#DCF8C6";

    public static void addFile(VBox chatBox, String iconPath, String fileName, boolean isSentByUser) {
        VBox messageBox = new VBox(2);

        // Message container
        VBox fileBubble = new VBox(5);
        fileBubble.setPadding(new Insets(8));
        fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? SENT_BG_COLOR : RECEIVED_BG_COLOR) + ";" +
                "-fx-background-radius: 15; -fx-border-radius: 15;");
        fileBubble.setMaxWidth(MAX_WIDTH);

        // Top line with icon + filename
        HBox fileInfoLine = new HBox(10);
        ImageView fileIcon = new ImageView();

        // Load image safely
        File iconFile = new File(iconPath);
        if (iconFile.exists()) {
            Image image = new Image(iconFile.toURI().toString());
            fileIcon.setImage(image);
            fileIcon.setFitWidth(35);
            fileIcon.setFitHeight(35);
            fileIcon.setPreserveRatio(true);
        } else {
            System.out.println("Icon file not found: " + iconPath);
        }

        VBox fileDetails = new VBox(2);
        Text fileNameText = new Text(truncateFileName(fileName, 30));
        fileNameText.setWrappingWidth(MAX_WIDTH);
        fileNameText.setStyle("-fx-font-size: 14px;");

        Text fileType = new Text("Document");
        fileType.setStyle("-fx-fill: #667781; -fx-font-size: 12px;");

        fileDetails.getChildren().addAll(fileNameText, fileType);
        fileInfoLine.getChildren().addAll(fileIcon, fileDetails);

        // Timestamp line
        HBox bottomLine = new HBox();
        bottomLine.setAlignment(Pos.CENTER_RIGHT);
        Text timeStamp = new Text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeStamp.setStyle("-fx-fill: #667781; -fx-font-size: 11px;");
        bottomLine.getChildren().add(timeStamp);

        Region spacer = new Region();
        spacer.setMinHeight(5);

        fileBubble.getChildren().addAll(fileInfoLine, spacer, bottomLine);

        // Hover effect
        fileBubble.setOnMouseEntered(e -> fileBubble.setStyle(
                "-fx-background-color: " + (isSentByUser ? SENT_HOVER_COLOR : RECEIVED_HOVER_COLOR) + ";" +
                        "-fx-background-radius: 15; -fx-border-radius: 15; -fx-cursor: hand;"
        ));

        fileBubble.setOnMouseExited(e -> fileBubble.setStyle(
                "-fx-background-color: " + (isSentByUser ? SENT_EXIT_COLOR : RECEIVED_BG_COLOR) + ";" +
                        "-fx-background-radius: 15; -fx-border-radius: 15; -fx-cursor: hand;"
        ));

        // Optional click action
        fileBubble.setOnMouseClicked(e -> {
            System.out.println("Clicked on file: " + fileName);
            // TODO: ouvrir ou télécharger le fichier
        });

        messageBox.getChildren().add(fileBubble);
        Platform.runLater(() -> chatBox.getChildren().add(messageBox));
    }

    // Helper: truncate long filenames
    private static String truncateFileName(String name, int maxLength) {
        return name.length() > maxLength ? name.substring(0, maxLength - 3) + "..." : name;
    }
}
