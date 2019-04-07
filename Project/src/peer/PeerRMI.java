package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerRMI extends Remote {

    public void backup(String filename, int degree) throws RemoteException;

    public void restore(String filename) throws RemoteException;

    public void delete() throws RemoteException;

    public void reclaim() throws RemoteException;

    public void state() throws RemoteException;

}