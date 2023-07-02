package RMI;

import java.rmi.RemoteException;

public interface ChatClient1Interface extends java.rmi.Remote {
    void receiveMessage(String message) throws RemoteException;
}
