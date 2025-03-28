package com.alaanya.controller;

import java.text.MessageFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class MessageController{

        public static void addMessage(@SuppressWarnings("exports") VBox chatBox, String senderId, String message, boolean isSentByUser) {
                VBox messageBox = new VBox(8);

                // Création d'une bulle de message
                VBox messageBubble = new VBox(4);
                messageBubble.setPadding(new Insets(12));
                messageBubble.setStyle(String.format(
                        "-fx-background-color: %s; -fx-background-radius: 15; -fx-border-radius: 15;",
                        isSentByUser ? "#DCF8C6" : "#FFFFFF"
                ));

                // Texte du message
                Text messageText = new Text(message);
                messageText.setWrappingWidth(250); // Retour à la ligne si nécessaire
                messageText.setStyle("-fx-font-size: 14px; -fx-font-family: 'Arial';");

                // Heure d'envoi
                String timeStamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                Text timeText = new Text(timeStamp);
                timeText.setStyle("-fx-fill: #666666; -fx-font-size: 10px;");

                // Nom de l'expéditeur
                TextFlow userName = new TextFlow(new Text(isSentByUser ? "Vous" : senderId));
                userName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-fill: #333333;");

                // Alignement dynamique
                if (isSentByUser) {
                        userName.setTextAlignment(TextAlignment.RIGHT);
                        messageBubble.setAlignment(Pos.CENTER_RIGHT);
                } else {
                        userName.setTextAlignment(TextAlignment.LEFT);
                        messageBubble.setAlignment(Pos.CENTER_LEFT);
                }

                // Assemblage final
                messageBubble.getChildren().addAll(messageText, timeText);
                messageBox.getChildren().addAll(userName, messageBubble);

                chatBox.getChildren().add(messageBox);
        }

}