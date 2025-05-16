package controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        LocalDate messageDate;
        LocalDateTime dateTime;
        System.out.println("timstamp  type ; "+timestamp.getClass());
        if (timestamp instanceof java.sql.Timestamp) {
            
            // Cas idéal : date + heure présentes
            dateTime = ((java.sql.Timestamp) timestamp).toLocalDateTime();
            messageDate = dateTime.toLocalDate();
        } else if (timestamp instanceof java.sql.Date) {
            // java.sql.Date ne contient que la date, pas l'heure
            messageDate = ((java.sql.Date) timestamp).toLocalDate();
            // Heure par défaut à minuit (00:00)
            dateTime = messageDate.atStartOfDay();
        } else if (timestamp != null) {
            // Cas général : java.util.Date
            dateTime = timestamp.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();
            messageDate = dateTime.toLocalDate();
        } else {
            // Si timestamp est null, on utilise la date et heure actuelles
            dateTime = LocalDateTime.now();
            messageDate = dateTime.toLocalDate();
        }

        // Gestion du séparateur de date
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

        // Création de la bulle de message
        VBox messageBox = new VBox(4);
        messageBox.setAlignment(isSentByUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Nom de l'expéditeur
        Text senderText = new Text(isSentByUser ? "Vous" : senderId);
        senderText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-fill: #333333;");
        TextFlow userName = new TextFlow(senderText);
        userName.setTextAlignment(isSentByUser ? TextAlignment.RIGHT : TextAlignment.LEFT);

        // Conteneur du message
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

        // Heure du message formatée
        String timeStamp = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        Text timeText = new Text(timeStamp);
        timeText.setStyle("-fx-fill: #666666; -fx-font-size: 10px;");

        // Conteneur heure + statut
        HBox timeBox = new HBox(4);
        timeBox.setAlignment(Pos.CENTER_RIGHT);
        timeBox.getChildren().add(timeText);

        // Icône de statut pour l'expéditeur
        if (status == null) status = "read";
        if (isSentByUser) {
            ImageView statusIcon = new ImageView(getStatusIcon(status));
            statusIcon.setFitWidth(12);
            statusIcon.setFitHeight(12);
            timeBox.getChildren().add(statusIcon);
        }

        // Assemblage des composants
        messageBubble.getChildren().addAll(messageText, timeBox);
        messageBox.getChildren().addAll(userName, messageBubble);
        
        Platform.runLater(() -> chatBox.getChildren().add(messageBox));
    }

    private static Image getStatusIcon(String status) {
        String path = switch (status) {
            case "read" -> "/com/alaanya/view/images/read.png";
            case "delivered" -> "/com/alaanya/view/images/delivered.png";
            default -> "/com/alaanya/view/images/send.png";
        };
        return new Image(Objects.requireNonNull(MessageController.class.getResourceAsStream(path)));
    }

    private static String getDateLabel(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) {
            return "Aujourd'hui";
        } else if (date.equals(today.minusDays(1))) {
            return "Hier";
        } else {
            return date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy"));
        }
    }
}
