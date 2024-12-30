## ALAANYA Communication System

**ALAANYA** is a versatile communication system designed for secure and efficient client-server interactions. It includes functionalities for real-time text messaging, file transfers, audio calls, and persistent data storage. This project is implemented using Java for both the server and client components.

---

## Features

### Server-side Features

- **Client Management**:

  - Handles multiple client connections simultaneously.
  - Stores client details in a MySQL database.

- **Messaging System**:

  - Broadcasts messages to all clients or sends them to specific clients.
  - Logs messages and file transfers in a database.

- **File Transfer**:

  - Supports file sharing between clients.
  - Saves transferred files and logs their metadata in the database.

- **Audio Calls**:

  - Enables real-time audio communication between clients.

### Client-side Features

- **User Interface**:

  - Intuitive chat UI with options for text and file messaging.
  - Visual representation of sent and received messages.

- **Real-time Communication**:

  - Sends and receives messages via a persistent socket connection.

- **File Sharing**:

  - Allows users to share files with other clients.

- **Audio Calls**:

  - Provides easy initiation and reception of audio calls.

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK)**: Version 11 or higher.
- **MySQL Server**:
  - Ensure a MySQL database is running.
  - Update the `Database` class in `Server.java` with your MySQL credentials.
- **JavaFX SDK**: For client-side graphical components.

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/Delmat237/ALAANYA_COM
   cd ALAANYA_COM
   ```

2. Set up the database:

   - The server automatically creates the necessary database and tables if they do not exist.

3. Compile the server and client:

   ```bash
   javac --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -d bin src/main/java/alaanya/*.java
   ```

4. Run the server:

   ```bash
   java -cp bin alaanya.AudioRelayServer
   java -cp bin alaanya.FileRelayServer
   java -cp bin alaanya.Server
   ```

5. Run the client:

   ```bash
   java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp bin alaanya.ChatApp
   ```

---

## File Structure

```plaintext
alaanya/
├── Server.java        # Main server implementation
├── ChatApp.java       # Client application (UI and communication logic)
├── audio/
│   ├── AudioReceiverThread.java  # Handles incoming audio call transfers
│   ├── AudioSetup.java           # Audio configuration setup
│   └── AudioSendThread.java      # Manages outgoing audio call transfers
├── file/
│   ├── FileReceiverThread.java  # Handles incoming file transfers
│   └── FileSendThread.java      # Manages outgoing file transfers
├── message/
│   ├── MessageReadThread.java   # Reads incoming messages
│   └── MessageWriteThread.java  # Sends outgoing messages
└── resources/
    └── icons/                   # Icons for the client UI
```

---

## Usage
## Application Preview

Here's a preview of the Alaanya Communication System:

![App Screenshot](assets/app-screenshot.png)


### Server Configuration

- **Ports**:

  - Text communication: `12345`
  - Audio communication: `12346`
  - File sharing: `12347`

- **Database**:

  - Host: `localhost`
  - User: `delmat`
  - Password: `azaleodel`

### Client Interaction

1. Launch the ChatApp client.
2. Connect to the server using the default hostname (`localhost`) or the hostname of the server's machine and port (`12345`).
3. Use the interface to:
   - Send text messages to connected clients.
   - Share files.
   - Initiate or receive audio calls.

### Setting Up JavaFX

1. **Download JavaFX SDK**:

   - Visit [JavaFX Downloads](https://gluonhq.com/products/javafx/) and download the appropriate version for your system.

2. **Compile and Run with JavaFX**:

   - Use `--module-path` to specify the JavaFX library path during compilation and execution.

   **Compile Command**:

   ```bash
   javac --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -d bin src/main/java/alaanya/*.java
   ```

   **Run Command**:

   ```bash
   java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp bin alaanya.ChatApp
   ```

3. **Test JavaFX Configuration**:
   Create a `HelloFX.java` file and test your setup:

   ```java
   import javafx.application.Application;
   import javafx.scene.Scene;
   import javafx.scene.control.Label;
   import javafx.stage.Stage;

   public class HelloFX extends Application {
       @Override
       public void start(Stage stage) {
           Label label = new Label("Hello, JavaFX!");
           Scene scene = new Scene(label, 400, 300);
           stage.setScene(scene);
           stage.setTitle("HelloFX");
           stage.show();
       }

       public static void main(String[] args) {
           launch();
       }
   }
   ```

   Compile and run it using the JavaFX setup instructions.

---

## Contributions

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Submit a pull request.

---

## License

This project is licensed under the MIT License. See the LICENSE file for details.

---

## Contact

For any inquiries, feel free to reach out:

- ![Gmail](https://img.icons8.com/color/48/000000/gmail--v1.png) [azangueleonel9@gmail.com](mailto:azangueleonel9@gmail.com)
- ![WhatsApp](https://img.icons8.com/color/48/000000/whatsapp.png) +237 657450314


