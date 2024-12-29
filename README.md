# ALAANYA Communication System

ALAANYA is a versatile communication system designed for secure and efficient client-server interactions. It includes functionalities for real-time text messaging, file transfers, and persistent data storage. This project is implemented using Java for both the server and client components.

## Features

### Server-side Features
- **Client Management:**
  - Handles multiple client connections simultaneously.
  - Stores client details in a MySQL database.
- **Messaging System:**
  - Broadcasts messages to all clients or sends them to specific clients.
  - Logs messages and file transfers in a database.
- **File Transfer:**
  - Supports file sharing between clients.
  - Saves transferred files and logs their metadata in the database.
 -  **Audio Transfer:**
  - Supports Audio call sharing between clients.
  

### Client-side Features
- **User Interface:**
  - Intuitive chat UI with options for text and file messaging.
  - Visual representation of sent and received messages.
- **Real-time Communication:**
  - Sends and receives messages via a persistent socket connection.
- **File Sharing:**
  - Allows users to share files with other clients.

## Getting Started

### Prerequisites
- **Java Development Kit (JDK):** Version 11 or higher.
- **MySQL Server:**
  - Ensure a MySQL database is running.
  - Update the `Database` class in `Server.java` with your MySQL credentials.
- **JavaFX SDK:** For client-side graphical components.

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
   javac -d bin src/alaanya/*.java
   ```

4. Run the server:
   ```bash
   java -cp bin alaanya.Server
   ```

5. Run the client:
   ```bash
   java -cp bin alaanya.ChatApp
   ```

## File Structure
```
alaanya/
├── Server.java        # Main server implementation
├── ChatApp.java       # Client application (UI and communication logic)
├── audio/
│   ├── AudioReceiverThread.java  # Handles incoming audio call transfers
|.  └── AufioSetup.java 
│   └── AudioSendThread.java      # Manages outgoing auduo call transfers
├── file/
│   ├── FileReceiverThread.java  # Handles incoming file transfers
│   └── FileSendThread.java      # Manages outgoing file transfers
├── message/
│   ├── MessageReadThread.java   # Reads incoming messages
│   └── MessageWriteThread.java  # Sends outgoing messages
└── resources/
    └── icons/                   # Icons for the client UI
```

## Usage

### Server Configuration
- **Port:** The server listens on port `12345` by default.
- **AudioPort:** The Audio server listens on port `12346` by defaul
- **FilePort:** The File server listens on port `12347` by default.
- **Database:**
  - Host: `localhost`
  - User: `delmat`
  - Password: `azaleodel`

### Client Interaction
1. Launch the `ChatApp` client.
2. Connect to the server using the default hostname (`localhost`) or the hostame of server's machineand port (`12345`).
3. Use the interface to send messages or files to connected clients.

## Contributions
Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Submit a pull request.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact
For any inquiries, contact **azangueleonel9@gmail.com**.
