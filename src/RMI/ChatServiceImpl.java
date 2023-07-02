package RMI;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class ChatServiceImpl extends UnicastRemoteObject implements ChatService {
    private List<ChatClientInterface> clients;

    public ChatServiceImpl() throws RemoteException {
        clients = new ArrayList<>();
    }

    public void sendMessage(String message) throws RemoteException {
        System.out.println("Received message: " + message);
        for (ChatClientInterface client : clients) {
            client.receiveMessage(message);
        }
    }

    public void registerClient(ChatClientInterface client) throws RemoteException {
        clients.add(client);
    }

    public void unregisterClient(ChatClientInterface client) throws RemoteException {
        clients.remove(client);
    }

    public static void main(String[] args) {
        try {
            int port = 12345;

            ChatService chatService = new ChatServiceImpl();
            LocateRegistry.createRegistry(port);
            Naming.rebind("rmi://localhost:" + port + "/ChatService", chatService);

            System.out.println("Chat Server started on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
