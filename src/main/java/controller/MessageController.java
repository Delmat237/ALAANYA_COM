package controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class MessageController {

        private static final double MAX_WIDTH = 250;
        private static LocalDate lastMessageDate = null;

        public static void addMessage(VBox chatBox, String senderId, String message, boolean isSentByUser, String status, Date timestamp) {
                // Vérifie si on doit insérer un séparateur de date
                LocalDate messageDate = LocalDate.parse(timestamp.toString());
                if (lastMessageDate == null || !lastMessageDate.equals(messageDate)) {
                        String dateLabel = getDateLabel(messageDate);
                        Text dateSeparator = new Text(dateLabel);
                        dateSeparator.setStyle("-fx-font-size: 11px; -fx-fill: gray; -fx-font-weight: bold;");

                        HBox dateBox = new HBox(dateSeparator);
                        dateBox.setAlignment(Pos.CENTER);
                        dateBox.setPadding(new Insets(10, 0, 10, 0));

                        Platform.runLater(() -> chatBox.getChildren().add(dateBox));
                        lastMessageDate = messageDate;
                }

                // Conteneur principal
                VBox messageBox = new VBox(4);
                messageBox.setAlignment(isSentByUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                Text senderText = new Text(isSentByUser ? "Vous" : senderId);
                senderText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-fill: #333333;");
                TextFlow userName = new TextFlow(senderText);
                userName.setTextAlignment(isSentByUser ? TextAlignment.RIGHT : TextAlignment.LEFT);

                VBox messageBubble = new VBox(4);
                messageBubble.setPadding(new Insets(12));
                messageBubble.setStyle(
                        "-fx-background-color: " + (isSentByUser ? "#DCF8C6" : "#FFFFFF") + ";" +
                                "-fx-background-radius: 15; -fx-border-radius: 15;"
                );
                messageBubble.setMaxWidth(MAX_WIDTH);
                messageBubble.setAlignment(isSentByUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                Text messageText = new Text(message);
                messageText.setWrappingWidth(MAX_WIDTH);
                messageText.setStyle("-fx-font-size: 14px; -fx-font-family: 'Arial';");

            timestamp.toString();
            String timeStamp = String.format(String.valueOf(DateTimeFormatter.ofPattern("HH:mm")));
                Text timeText = new Text(timeStamp);
                timeText.setStyle("-fx-fill: #666666; -fx-font-size: 10px;");

                HBox timeBox = new HBox(4);
                timeBox.setAlignment(Pos.CENTER_RIGHT);
                timeBox.getChildren().add(timeText);

                if (status == null) status = "read";
                if (isSentByUser) {
                        ImageView statusIcon = new ImageView(getStatusIcon(status));
                        statusIcon.setFitWidth(12);
                        statusIcon.setFitHeight(12);
                        timeBox.getChildren().add(statusIcon);
                }

                messageBubble.getChildren().addAll(messageText, timeBox);
                messageBox.getChildren().addAll(userName, messageBubble);

                Platform.runLater(() -> chatBox.getChildren().add(messageBox));
        }

        private static Image getStatusIcon(String status) {
                String path;
                switch (status) {
                        case "read":
                                path = "/com/alaanya/view/images/read.png";
                                break;
                        case "delivered":
                                path = "/com/alaanya/view/images/delivered.png";
                                break;
                        default:
                                path = "/com/alaanya/view/images/send.png";
                                break;
                }
                return new Image(Objects.requireNonNull(MessageController.class.getResourceAsStream(path)));
        }
        private static String getDateLabel(LocalDate date) {
                LocalDate today = LocalDate.now();
                if (date.equals(today)) {
                        return "Aujourd'hui";
                } else if (date.equals(today.minusDays(1))) {
                        return "Hier";
                } else {
                        return date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")); // ex : "Lundi 06 Mai 2025"
                }
        }

}
