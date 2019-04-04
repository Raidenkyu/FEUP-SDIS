package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

interface PeerRMI extends Remote {

    public void backup(String filename, int degree) throws RemoteException;

    public void restore() throws RemoteException;

    public void delete() throws RemoteException;

    public void reclaim() throws RemoteException;

    public void state() throws RemoteException;

}