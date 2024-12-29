package file;

import alaanya.ChatApp;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class FileSendThread extends  Thread{
    private final Socket socket;
    private ChatApp chatApp;
    private DataOutputStream out;

    public FileSendThread(ChatApp chatApp,Socket socket) throws IOException {
        this.socket = socket;
        this.chatApp = chatApp;
        out = new DataOutputStream(this.socket.getOutputStream());

    }

    public void send(File file, ProgressBar progressBar) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {

            long totalBytes = file.length();
            long bytesSent = 0;
            byte[] buffer = new byte[8192];
            int bytesRead;

            Platform.runLater(() -> progressBar.setProgress(0));

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesSent += bytesRead;

                double progress = (double) bytesSent / totalBytes;
                Platform.runLater(() -> progressBar.setProgress(progress));
            }

            out.flush();
            Platform.runLater(() ->this.chatApp.addFile(this.chatApp.getChatBox(this.chatApp.currentContactId), "/icons/document-signed.png", file.getName(), true));

            System.out.println("Fichier envoyé avec succès : " + file.getName());

        } catch (IOException e) {
            System.out.println("Erreur lors de l'envoi du fichier : " + e.getMessage());
        }
    }



}

