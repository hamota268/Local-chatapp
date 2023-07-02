package ChatApp;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private List<OutputStream> outputStreams;
    private Map<String, String> emojiMap;
    private ServerSocket serverSocket;
    private int port;

    private boolean isConnected(){
        return !serverSocket.isClosed();
    }

    public ChatServer(int port) {
        this.port = port;
        outputStreams = new ArrayList<>();
        emojiMap = createEmojiMap();
        startServer();
        startCommandListener();

    }
    private Map<String, String> createEmojiMap(){
        Map<String, String> map = new HashMap<>();
        map.put(":)", "\uD83D\uDE42"); // Example mapping for a smiling face emoji
        map.put(":D", "\uD83D\uDE00"); // Example mapping for a grinning face emoji
        return map;
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Chat Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());

                // Start a new thread to handle the client
                Thread clientThread = new Thread(new ClientHandler(socket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restartServer() {
        try {
            serverSocket.close();
            System.out.println("Server stopped");
            outputStreams.clear();
            startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startCommandListener() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type 'restart' to restart the server. Type 'quit' to exit.");

        while (true) {
            String command = scanner.nextLine().trim();
            if (command.equalsIgnoreCase("restart")) {
                restartServer();
            } else if (command.equalsIgnoreCase("quit")) {
                System.exit(0);
            } else {
                System.out.println("Invalid command. Please try again.");
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private DataInputStream input;
        private OutputStream output;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                input = new DataInputStream(socket.getInputStream());
                output = socket.getOutputStream();

                outputStreams.add(output);

                String message;
                while ((message = input.readLine()) != null) {
                    if (message.startsWith("text:")) {
                        String textMessage = message.substring(5);
                        if(textMessage.equals("restartserverinit")){
                            restartServer();
                            break;
                        }else{
                            broadcast(textMessage, output);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    outputStreams.remove(output);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        private String replaceEmojiText(String message){
            for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
                String emojiText = entry.getKey();
                String emojiChar = entry.getValue();
                message = message.replace(emojiText, emojiChar);
            }
            return message;
        }

        private void broadcast(String message, OutputStream excludeStream) {
            for (OutputStream stream : outputStreams) {
                if (stream != excludeStream) {
                    try {
                        PrintWriter writer = new PrintWriter(stream);
                        String emojiMessage = replaceEmojiText(message);
                        writer.println(message);
                        writer.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        int port = 12345; // Specify the desired port number
        new ChatServer(port);
    }
}
