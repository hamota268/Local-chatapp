package RMI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class ChatClient1 extends JFrame implements ChatClient1Interface {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;

    private ChatService chatService;

    public ChatClient1(String serverAddress, int serverPort) {
        try {
            chatService = (ChatService) Naming.lookup("rmi://" + serverAddress + ":" + serverPort + "/ChatService");
            chatService.registerClient((ChatClientInterface) this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(400, 300);
    }

    public void receiveMessage(String message) throws RemoteException {
        SwingUtilities.invokeLater(() -> {
            appendToChatArea(message);
        });
    }

    private void appendToChatArea(String message) {
        chatArea.append(message + "\n");
    }

    private void sendMessage() {
        String message = messageField.getText();
        try {
            chatService.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        appendToChatArea("You: " + message);
        messageField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String serverAddress = "localhost";
            int serverPort = 12345;
            ChatClient1 client = new ChatClient1(serverAddress, serverPort);
            client.setVisible(true);
        });
    }
}
