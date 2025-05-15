package controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FileController {
    private static final double MAX_WIDTH = 250;
    private static final String SENT_BG_COLOR = "#0C5DAFFF";
    private static final String RECEIVED_BG_COLOR = "#FFFFFF";
    private static final String SENT_HOVER_COLOR = "#c5e1b0";
    private static final String RECEIVED_HOVER_COLOR = "#f5f5f5";
    private static final String SENT_EXIT_COLOR = "#DCF8C6";

    public static void addFile(VBox chatBox, String iconPath, String filePathOrName, boolean isSentByUser) {
        VBox messageBox = new VBox(2);

        VBox fileBubble = new VBox(5);
        fileBubble.setPadding(new Insets(8));
        fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? SENT_BG_COLOR : RECEIVED_BG_COLOR) + ";" +
                "-fx-background-radius: 15; -fx-border-radius: 15;");
        fileBubble.setMaxWidth(MAX_WIDTH);

        HBox fileInfoLine = new HBox(10);
        ImageView fileIcon = new ImageView();

        String iconFileName = switch (getFileExtension(filePathOrName)) {
            case "jpg", "jpeg", "png", "gif", "bmp" -> "image.png";
            case "mp4", "avi", "mkv" -> "video.png";
            case "mp3", "wav", "ogg" -> "audio.png";
            case "pdf" -> "pdf.png";
            case "doc", "docx" -> "word.png";
            case "xls", "xlsx" -> "excel.png";
            case "ppt", "pptx" -> "ppt.png";
            case "txt", "md" -> "text.png";
            case "zip", "rar", "7z" -> "archive.png";
            default -> "file.png";
        };
        InputStream iconStream = FileController.class.getResourceAsStream("/com/alaanya/view/images/" + iconFileName);
        if (iconStream != null) {
            Image image = new Image(iconStream);
            fileIcon.setImage(image);
            fileIcon.setFitWidth(35);
            fileIcon.setFitHeight(35);
            fileIcon.setPreserveRatio(true);
        }

        VBox fileDetails = new VBox(2);
        Text fileNameText = new Text(truncateFileName(filePathOrName));
        fileNameText.setWrappingWidth(MAX_WIDTH);
        fileNameText.setStyle("-fx-font-size: 14px;");

        Text fileType = new Text(getFileTypeDescription(filePathOrName));
        fileType.setStyle("-fx-fill: #667781; -fx-font-size: 12px;");
        fileDetails.getChildren().addAll(fileNameText, fileType);
        fileInfoLine.getChildren().addAll(fileIcon, fileDetails);

        HBox bottomLine = new HBox();
        bottomLine.setAlignment(Pos.CENTER_RIGHT);
        Text timeStamp = new Text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeStamp.setStyle("-fx-fill: #667781; -fx-font-size: 11px;");
        bottomLine.getChildren().add(timeStamp);

        Region spacer = new Region();
        spacer.setMinHeight(5);

        fileBubble.getChildren().addAll(fileInfoLine, spacer, bottomLine);

        final String baseStyle = "-fx-background-radius: 15; -fx-border-radius: 15;";
        fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? SENT_BG_COLOR : RECEIVED_BG_COLOR) + ";" + baseStyle);

        fileBubble.setOnMouseEntered(e -> fileBubble.setStyle(
                "-fx-background-color: " + (isSentByUser ? SENT_HOVER_COLOR : RECEIVED_HOVER_COLOR) + ";" + baseStyle)
        );
        fileBubble.setOnMouseExited(e -> fileBubble.setStyle(
                "-fx-background-color: " + (isSentByUser ? SENT_EXIT_COLOR : RECEIVED_BG_COLOR) + ";" + baseStyle)
        );

        fileBubble.setOnMouseClicked(e -> {
            String downloadPath =  "Downloads" + File.separator + filePathOrName;
            openLocalFile(downloadPath);
        });
        
        messageBox.getChildren().add(fileBubble);
        Platform.runLater(() -> chatBox.getChildren().add(messageBox));
    }

    private static String truncateFileName(String name) {
        return name.length() > 30 ? name.substring(0, 30 - 3) + "..." : name;
    }

    // Ouvrir un fichier local
    private static void openLocalFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert("Fichier introuvable", "Le fichier n'existe pas à l'emplacement spécifié.");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le fichier.");
        }
    }

    // Sauvegarder un fichier reçu puis l’ouvrir
    private static void saveAndOpenReceivedFile(String fileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(fileName);
        fileChooser.setTitle("Enregistrer le fichier");

        File targetFile = fileChooser.showSaveDialog(null);
        if (targetFile != null) {
            try {
                // Simule ici la récupération du fichier (dans un vrai cas, lire le fichier transmis)
                // Pour la démo, on crée un fichier texte vide
                Files.write(targetFile.toPath(), "Contenu du fichier reçu".getBytes());

                Desktop.getDesktop().open(targetFile);
            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible d'enregistrer ou ouvrir le fichier.");
            }
        }
    }

    private static void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }


    private static String getFileTypeDescription(String fileName) {
        String ext = "";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            ext = fileName.substring(i + 1).toLowerCase();
        }

        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "bmp", "svg" -> "Image";
            case "mp4", "avi", "mkv", "mov" -> "Vidéo";
            case "mp3", "wav", "ogg", "flac" -> "Audio";
            case "pdf" -> "PDF";
            case "doc", "docx" -> "Word";
            case "xls", "xlsx" -> "Excel";
            case "ppt", "pptx" -> "PowerPoint";
            case "txt", "md", "rtf" -> "Texte";
            case "zip", "rar", "7z", "tar", "gz" -> "Archive";
            case "apk" -> "Application Android";
            case "exe" -> "Application Windows";
            default -> "Document";
        };
    }

}
