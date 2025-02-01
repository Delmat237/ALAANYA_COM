package alaanya;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class AudioRelayServer extends ServerSocket {
    private final ConcurrentHashMap<String, Socket> clients = new ConcurrentHashMap<>();
    AudioFormat format = new AudioFormat(44100, 16, 1, true,false);
    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
    SourceDataLine speakers;

    public AudioRelayServer(int AUDIO_PORT) throws IOException {
        super(AUDIO_PORT);
            System.out.println("Audio Relay Server démarré sur le port " + AUDIO_PORT);

            try{
            while (true) {
                Socket clientSocket = this.accept();
                System.out.println("Client connecté : " + clientSocket.getInetAddress());

                // Thread pour gérer le client
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur audio : " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        String clientId = clientSocket.getInetAddress().getHostAddress();
        try {
            clients.put(clientId, clientSocket); // Ajouter le client
            InputStream inputStream = clientSocket.getInputStream();
            byte[] buffer = new byte[1024];
            //Ouvrir un SourceDataLine pour jouer l'audio

            try {
                speakers = (SourceDataLine) AudioSystem.getLine(info);


                speakers.open(format);
                speakers.start();

                while (!clientSocket.isClosed()) {
                    int bytesRead = inputStream.read(buffer, 0, buffer.length);
                    if (bytesRead == -1) break; // Déconnexion du client

                    String message = new String(buffer, 0, bytesRead);
                    System.out.println("buffer intermediare " + buffer);

                    //Verifie si le message est une commande spéciale

                    if (message.startsWith("START_CALL:")) {
                        System.out.println("rexr"+bytesRead);
                        String receiverId = message.split(":")[1];
                        handleCallRequest(clientId, receiverId);
                    } else {
                        //Sinon , relayer les paquets audio
                        System.out.println("audio"+bytesRead);
                        //speakers.write(buffer, 0, bytesRead);
                        relayAudio(buffer, bytesRead, clientId);
                    }
                }
            } catch (LineUnavailableException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            System.err.println("Erreur avec le client " + clientId + " : " + e.getMessage());
        } finally {
            // Supprimer le client et fermer les ressources
            clients.remove(clientId);
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
            System.out.println("Client déconnecté : " + clientId);
        }
    }

    private void relayAudio(byte[] buffer, int length, String senderId) {
        for (String clientId : clients.keySet()) {
            if (!clientId.equals(senderId)) {
                try {
                    Socket clientSocket = clients.get(clientId);
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        OutputStream outputStream = clientSocket.getOutputStream();
                        outputStream.write(buffer, 0, length);
                       // speakers.write(buffer, 0, length);
                        System.out.println("recu :"+buffer);
                    }
                } catch (IOException e) {
                    System.err.println("Erreur lors du relais audio vers le client " + clientId + " : " + e.getMessage());
                    clients.remove(clientId); // Supprimer le client problématique
                }
            }
        }
    }

    public void notifyClientOfIncomingCall(String callerId, String receiverId) {
        try{
            Socket receiverSocket = clients.get(receiverId); //Obtenir le socket du destinataire
            if (receiverSocket != null && !receiverSocket.isClosed()) {
                OutputStream out = receiverSocket.getOutputStream();
                String notification = "CALL_INCOMING: " + callerId + " \n" ;
                out.write(notification.getBytes());
                out.flush();
                }
        } catch (IOException e) {
            System.err.println("Erreur lors de la notification d'appel entrant à " + receiverId + " : " + e.getMessage());
        }
    }


    //Requete de demarrage d'appel
    private void handleCallRequest(String callerId, String receiverId) {
        if (clients.containsKey(receiverId)) {
            notifyClientOfIncomingCall(callerId, receiverId);
        }
        else{
            System.err.println("Le destinataire " +receiverId+ " n'est pas connecté .");
            notifyClientOfIncomingCallerUnavailable(callerId, receiverId);
        }
    }

    //Notifier le client appelant si le destinataire est indisponible

    private void notifyClientOfIncomingCallerUnavailable(String callerId, String receiverId) {
        try {
            Socket callerSocket = clients.get(callerId);
            if (callerSocket != null && !callerSocket.isClosed()) {
                OutputStream out = callerSocket.getOutputStream();
                String notification = "CALL_INCOMING: " + receiverId + " \n" ;
                out.write(notification.getBytes());
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la notification  au client " + callerId + " : " + e.getMessage());
        }
    }
    public static void main(String [] args) throws IOException {
        new AudioRelayServer(12346);
    }
}
