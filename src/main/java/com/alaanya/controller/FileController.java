package com.alaanya.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FileController {
    // Icon size
    private static final double MAX_WIDTH = 250; // Max text width

    public static void addFile(VBox chatBox, String iconPath, String fileName, boolean isSentByUser) {
        VBox messageBox = new VBox(2);

        // Main bubble container
        VBox fileBubble = new VBox(5);
        fileBubble.setPadding(new Insets(8));
        fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? "#26631b" : "#FFFFFF") + ";"
                + "-fx-background-radius: 15; -fx-border-radius: 15;");

        fileBubble.setMaxWidth(250);
        // Top line with icon and filename
        HBox fileInfoLine = new HBox(10);

        ImageView fileIcon = new ImageView(); // Initialize first

        try {
            Image image = new Image("file:" + iconPath); // Proper path concatenation
            fileIcon.setImage(image);
            fileIcon.setFitWidth(35);
            fileIcon.setFitHeight(35);
            fileIcon.setPreserveRatio(true);
        } catch (Exception e) {
            System.out.println("Error loading image: " + e.getMessage());
            e.printStackTrace();
        }

        VBox fileDetails = new VBox(2);
        Text fileNameText = new Text(fileName);
        fileNameText.setWrappingWidth(MAX_WIDTH); // Set wrapping width directly
        fileNameText.setStyle("-fx-font-size: 14px;");

        Text fileSize = new Text("Document");
        fileSize.setStyle("-fx-fill: #667781; -fx-font-size: 12px;");

        fileDetails.getChildren().addAll(fileNameText, fileSize);
        fileInfoLine.getChildren().addAll(fileIcon, fileDetails);

        // Bottom line with timestamp
        HBox bottomLine = new HBox();
        bottomLine.setAlignment(Pos.CENTER_RIGHT);
        Text timeStamp = new Text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeStamp.setStyle("-fx-fill: #667781; -fx-font-size: 11px;");
        bottomLine.getChildren().add(timeStamp);

        Region spacer = new Region();
        spacer.setMinHeight(5);

        fileBubble.getChildren().addAll(fileInfoLine, spacer, bottomLine);

        // Hover effect
        fileBubble.setOnMouseEntered(e -> {
            fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? "#c5e1b0" : "#f5f5f5") + ";"
                    + "-fx-background-radius: 15; -fx-border-radius: 15;");
            fileBubble.setStyle(fileBubble.getStyle() + "-fx-cursor: hand;");
        });

        fileBubble.setOnMouseExited(e -> {
            fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? "#DCF8C6" : "#FFFFFF") + ";"
                    + "-fx-background-radius: 15; -fx-border-radius: 15;");
            fileBubble.setStyle(fileBubble.getStyle() + "-fx-cursor: hand;");
        });

        messageBox.getChildren().addAll(fileBubble);
        Platform.runLater(() -> chatBox.getChildren().add(messageBox));
    }

}
