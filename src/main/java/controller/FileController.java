package controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.awt.Desktop;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import utils.FilePreviewUtil;

import org.kordamp.ikonli.javafx.FontIcon;

public class FileController {
    // Constantes pour la mise en forme
    private static final double MAX_WIDTH = 250;

    private static final String SENT_BG_COLOR = "#0C5DAFFF";
    private static final String RECEIVED_BG_COLOR = "#FFFFFF";
    private static final String SENT_HOVER_COLOR = "#c5e1b0";
    private static final String RECEIVED_HOVER_COLOR = "#f5f5f5";
    private static final String SENT_EXIT_COLOR = "#DCF8C6";



    /**
     * Ajoute un fichier dans la boîte de discussion (chatBox)
     *
     * @param chatBox     conteneur VBox où le message sera ajouté
     * @param filePath    chemin du fichier (utilisé pour le preview)
     * @param fileName    nom du fichier à afficher
     * @param isSentByUser si true, message envoyé par l'utilisateur
     */
    public static void addFile(VBox chatBox, String filePath, String fileName, boolean isSentByUser) {
        VBox messageBox = new VBox(2);

        // Conteneur principal du fichier
        VBox fileBubble = new VBox(5);
        fileBubble.setPadding(new Insets(8));
        fileBubble.setMaxWidth(MAX_WIDTH);
        fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? SENT_BG_COLOR : RECEIVED_BG_COLOR) +
                "; -fx-background-radius: 15; -fx-border-radius: 15;");

        // Ligne supérieure avec l’icône, le nom, le type, et le bouton de téléchargement (si reçu)
        HBox fileInfoLine = new HBox(10);
        fileInfoLine.setAlignment(Pos.CENTER_LEFT);

        // Détermination de l'icône en fonction de l'extension du fichier
        String extension = getFileExtension(fileName);
        String iconLiteral = switch (extension) {
            case "jpg", "jpeg", "png", "gif", "bmp" -> "fas-file-image";
            case "mp4", "avi", "mkv" -> "fas-file-video";
            case "mp3", "wav", "ogg" -> "fas-file-audio";
            case "pdf" -> "fas-file-pdf";
            case "doc", "docx" -> "fas-file-word";
            case "xls", "xlsx" -> "fas-file-excel";
            case "ppt", "pptx" -> "fas-file-powerpoint";
            case "txt", "md", "rtf", "xml" -> "fas-file-alt";
            case "zip", "rar", "7z" -> "fas-file-archive";
            default -> "fas-file";
        };

        FontIcon fileIcon = new FontIcon(iconLiteral);
        fileIcon.setIconSize(35);

        // Informations : nom de fichier + type
        VBox fileDetails = new VBox(2);
        Text fileNameText = new Text(truncateFileName(fileName));
        fileNameText.setWrappingWidth(MAX_WIDTH);
        fileNameText.setStyle("-fx-font-size: 14px;");
        Text fileType = new Text(getFileTypeDescription(fileName));
        fileType.setStyle("-fx-fill: #667781; -fx-font-size: 12px;");
        fileDetails.getChildren().addAll(fileNameText, fileType);

        // Bouton de téléchargement uniquement si le fichier est reçu
        Button downloadButton = new Button();
        if (!isSentByUser) {
            FontIcon downloadIcon = new FontIcon("fas-download");
            downloadIcon.setIconSize(18);
            downloadButton.setGraphic(downloadIcon);
            downloadButton.setStyle("-fx-background-color: transparent;");
            downloadButton.setOnAction(e -> startDownload("Downloads/" + fileName));
        }

        // Ajout conditionnel des éléments dans la ligne du haut
        if (isSentByUser) {
            fileInfoLine.getChildren().addAll(fileIcon, fileDetails);
        } else {
            fileInfoLine.getChildren().addAll(fileIcon, fileDetails, downloadButton);
        }
        // Prévisualisation du fichier si applicable
        Node previewNode = FilePreviewUtil.createFilePreview(filePath, extension);

        // Ligne du bas avec l'heure
        HBox bottomLine = new HBox();
        bottomLine.setAlignment(isSentByUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        Text timeStamp = new Text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeStamp.setStyle("-fx-fill: #667781; -fx-font-size: 11px;");
        bottomLine.getChildren().add(timeStamp);

        // Ajout des composants dans la bulle
        if (previewNode != null) fileBubble.getChildren().add(previewNode);
        fileBubble.getChildren().addAll(fileInfoLine, new Region(), bottomLine);

        // Animation hover
        final String baseStyle = "-fx-background-radius: 15; -fx-border-radius: 15;";
        fileBubble.setOnMouseEntered(e -> fileBubble.setStyle("-fx-background-color: " +
                (isSentByUser ? SENT_HOVER_COLOR : RECEIVED_HOVER_COLOR) + ";" + baseStyle));
        fileBubble.setOnMouseExited(e -> fileBubble.setStyle("-fx-background-color: " +
                (isSentByUser ? SENT_EXIT_COLOR : RECEIVED_BG_COLOR) + ";" + baseStyle));

        // Double-clic ouvre le fichier
        fileBubble.setOnMouseClicked(e -> {
            Task<Void> openTask = new Task<>() {
                @Override
                protected Void call() {
                    try {
                        openLocalFile(filePath);
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Erreur", "Échec  de Lecture"));
                    }
                    return null;
                }
            };
            new Thread(openTask).start();
        });

        // Positionnement du message (gauche ou droite)
        HBox container = new HBox();
        Region horizontalSpacer = new Region();
        HBox.setHgrow(horizontalSpacer, Priority.ALWAYS);
        if (isSentByUser) {
            container.getChildren().addAll(horizontalSpacer, fileBubble);
        } else {
            container.getChildren().addAll(fileBubble, horizontalSpacer);
        }

        messageBox.getChildren().add(container);

        // Ajout au chat
        Platform.runLater(() -> chatBox.getChildren().add(messageBox));
    }

    /**
     * Lance un faux téléchargement avec barre de progression circulaire
     */
    private static void startDownload(String filePath) {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        Alert progressDialog = new Alert(Alert.AlertType.NONE);
        progressDialog.setGraphic(progressIndicator);
        progressDialog.setTitle("Téléchargement");
        progressDialog.getDialogPane().setContent(progressIndicator);
        progressDialog.setResizable(true);

        Task<Void> downloadTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                   // Thread.sleep(2); // Simulation
                    openLocalFile(filePath);
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Erreur", "Échec du téléchargement"));
                }
                return null;
            }

            @Override
            protected void succeeded() {
                progressDialog.close();
            }
        };

        new Thread(downloadTask).start();
        progressDialog.show();
    }



    /**
     * Ouvre un fichier local avec l'application par défaut
     */
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

    // Tronque les noms de fichiers trop longs
    private static String truncateFileName(String name) {
        return name.length() > 30 ? name.substring(0, 27) + "..." : name;
    }

    // Extrait l’extension
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < fileName.length() - 1)
                ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }

    // Retourne une description textuelle du type de fichier
    private static String getFileTypeDescription(String fileName) {
        String ext = getFileExtension(fileName);
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "bmp", "svg" -> "Image";
            case "mp4", "avi", "mkv", "mov" -> "Vidéo";
            case "mp3", "wav", "ogg", "flac" -> "Audio";
            case "pdf" -> "PDF";
            case "doc", "docx" -> "Word";
            case "xls", "xlsx" -> "Excel";
            case "ppt", "pptx" -> "PowerPoint";
            case "txt", "md", "rtf", "xml" -> "Texte";
            case "zip", "rar", "7z", "tar", "gz" -> "Archive";
            case "apk" -> "Application Android";
            case "exe" -> "Application Windows";
            default -> "Document";
        };
    }

    // Affiche une alerte standard
    public static void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }
}
