package controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class MessageController {

        private static final double MAX_WIDTH = 250;

        public static void addMessage(VBox chatBox, String senderId, String message, boolean isSentByUser) {
                // Conteneur principal
                VBox messageBox = new VBox(4);
                messageBox.setAlignment(isSentByUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                // Nom de l'expéditeur
                Text senderText = new Text(isSentByUser ? "Vous" : senderId);
                senderText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-fill: #333333;");
                TextFlow userName = new TextFlow(senderText);
                userName.setTextAlignment(isSentByUser ? TextAlignment.RIGHT : TextAlignment.LEFT);

                // Bulle de message
                VBox messageBubble = new VBox(4);
                messageBubble.setPadding(new Insets(12));
                messageBubble.setStyle(
                        "-fx-background-color: " + (isSentByUser ? "#DCF8C6" : "#FFFFFF") + ";" +
                                "-fx-background-radius: 15; -fx-border-radius: 15;"
                );
                messageBubble.setMaxWidth(MAX_WIDTH);
                messageBubble.setAlignment(isSentByUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                // Texte du message
                Text messageText = new Text(message);
                messageText.setWrappingWidth(MAX_WIDTH);
                messageText.setStyle("-fx-font-size: 14px; -fx-font-family: 'Arial';");

                // Heure d'envoi
                String timeStamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                Text timeText = new Text(timeStamp);
                timeText.setStyle("-fx-fill: #666666; -fx-font-size: 10px;");

                // Ajout des éléments
                messageBubble.getChildren().addAll(messageText, timeText);
                messageBox.getChildren().addAll(userName, messageBubble);

                // Affichage dans l'interface
                Platform.runLater(() -> chatBox.getChildren().add(messageBox));
        }
}
