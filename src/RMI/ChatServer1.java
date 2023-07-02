package RMI;

import java.rmi.*;
import java.rmi.registry.*;

public class ChatServer1 {
    public static void main(String[] args) {
        try {
            int serverPort = 1099;
            String chatServiceURL = "rmi://localhost:" + serverPort + "/ChatService";
            ChatService chatService = new ChatServiceImpl();
            LocateRegistry.createRegistry(serverPort);
            Naming.rebind(chatServiceURL, chatService);
            System.out.println("Chat server is running on port " + serverPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
