package ChatApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatClient extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton sendImageButton;
    private JButton sendAudioButton;
    private JButton reconnectButton;
    private Map<String, String> emojiMap;

    private Socket socket;
    private DataInputStream input;
    private PrintWriter output;
    private boolean isConnected() {
        return socket != null && socket.isConnected();
    }
    private static Map<String, String> createEmojiMap(){
        Map<String, String> map = new HashMap<>();
        map.put(":)", "\uD83D\uDE42"); // Example mapping for a smiling face emoji
        map.put(":D", "\uD83D\uDE00"); // Example mapping for a grinning face emoji
        // Add more emoji mappings as needed
        return map;
    }

    public ChatClient(String serverAddress, int serverPort, Map<String, String> emojiMap) {

        this.emojiMap = emojiMap;

        setTitle("first Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        emojiMap = createEmojiMap();

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendImageButton = new JButton("Send Image");
        sendAudioButton = new JButton("Send Audio");

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(sendImageButton, BorderLayout.WEST);
        inputPanel.add(sendAudioButton, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        sendImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendImage();
            }
        });

        sendAudioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendAudio();
            }
        });

        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        reconnectButton = new JButton("Reconnect");
        inputPanel.add(reconnectButton, BorderLayout.NORTH);

        reconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isConnected()) {
                    try {
                        socket.close();
                        connect(serverAddress, serverPort);
                    } catch (IOException ex) {
                        appendToChatArea("something went wrong!");
                    }
                }
            }
        });


        connect(serverAddress, serverPort);
    }


    public void connect(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new DataInputStream(socket.getInputStream());
            output = new PrintWriter(socket.getOutputStream(), true);

            appendToChatArea("Connected to the server: " + serverAddress + ":" + serverPort);

            reconnectButton.setEnabled(false); // Disable reconnect button on successful connection

            // Start a new thread to listen for messages from the server
            Thread messageListener = new Thread(new MessageListener());
            messageListener.start();
        } catch (IOException e) {
            appendToChatArea("Failed to connect to the server: " + serverAddress + ":" + serverPort);
            e.printStackTrace();
        }
    }


    private void sendMessage() {
        String message = messageField.getText();
        message = replaceEmojiText(message);
        output.println("text:" + message);
        appendToChatArea("You: " + message);
        messageField.setText("");
    }
    private String replaceEmojiText(String message){
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            String emojiText = entry.getKey();
            String emojiChar = entry.getValue();
            message = message.replace(emojiText, emojiChar);
        }
        return message;
    }

    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            byte[] imageData = readFileData(selectedFile);
            if (imageData != null) {
                output.println("image:" + selectedFile.getName());
                output.println(imageData.length);
                output.flush();
                try (BufferedOutputStream bufferedOutput = new BufferedOutputStream(socket.getOutputStream())) {
                    bufferedOutput.write(imageData, 0, imageData.length);
                    bufferedOutput.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendAudio() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            byte[] audioData = readFileData(selectedFile);
            if (audioData != null) {
                output.println("audio:" + selectedFile.getName());
                output.println(audioData.length);
                output.flush();
                try (BufferedOutputStream bufferedOutput = new BufferedOutputStream(socket.getOutputStream())) {
                    bufferedOutput.write(audioData, 0, audioData.length);
                    bufferedOutput.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private byte[] readFileData(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file);
             ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                byteOutputStream.write(buffer, 0, bytesRead);
            }
            return byteOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void appendToChatArea(String message) {
        chatArea.append(message + "\n");
    }

    private class MessageListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = input.readLine()) != null) {
                    if (message.startsWith("image:")) {
                        String imageName = message.substring(6);
                        int imageSize = Integer.parseInt(input.readLine());
                        byte[] imageData = new byte[imageSize];
                        input.readFully(imageData);
                        displayImage(imageName, imageData);
                    } else if (message.startsWith("audio:")) {
                        String audioName = message.substring(6);
                        int audioSize = Integer.parseInt(input.readLine());
                        byte[] audioData = new byte[audioSize];
                        input.readFully(audioData);
                        playAudio(audioName, audioData);
                    } else {
                        String sender = socket.getInetAddress().getHostAddress();
                        appendToChatArea(message);
                    }
                }
            } catch (IOException e) {
                appendToChatArea("Disconnected from the server.");
                reconnectButton.setEnabled(true); // Enable reconnect button on disconnection
                e.printStackTrace();
            }
        }
    }

    private void displayImage(String imageName, byte[] imageData) {
        try {
            // Save the image data to a temporary file
            File tempFile = File.createTempFile("image", null);
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                fileOutputStream.write(imageData);
            }

            // Open the image file with the default application
            Desktop.getDesktop().open(tempFile);

            // For demonstration purposes, let's just print the image name
            appendToChatArea("Received image: " + imageName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playAudio(String audioName, byte[] audioData) {
        try {
            // Save the audio data to a temporary file
            File tempFile = File.createTempFile("audio", null);
            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                fileOutputStream.write(audioData);
            }

            // Open the audio file with the default application
            Desktop.getDesktop().open(tempFile);

            // For demonstration purposes, let's just print the audio name
            appendToChatArea("Received audio: " + audioName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String serverAddress = "localhost";
            int serverPort = 12345;
            Map<String, String> emojiMap = createEmojiMap(); // Initialize the emoji map
            ChatClient client = new ChatClient(serverAddress, serverPort, emojiMap);
            client.setVisible(true);
        });
    }
}
