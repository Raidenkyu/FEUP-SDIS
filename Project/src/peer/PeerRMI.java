package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javafx.util.Pair;

public interface PeerRMI extends Remote {

    public void backup(String filename, int degree) throws RemoteException;

    public void restore(String filename) throws RemoteException;

    public void delete(String filename) throws RemoteException;

    public void reclaim(int diskSpace) throws RemoteException;

    public String state() throws RemoteException;

}