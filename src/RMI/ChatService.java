package RMI;

import java.rmi.RemoteException;

public interface ChatService extends java.rmi.Remote {
    void sendMessage(String message) throws RemoteException;

    void registerClient(ChatClientInterface client) throws RemoteException;

    void unregisterClient(ChatClientInterface client) throws RemoteException;

}
