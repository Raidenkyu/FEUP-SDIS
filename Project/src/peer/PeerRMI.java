package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface PeerRMI extends Remote {

    public void backup(String filename, int degree, boolean enhanced) throws RemoteException;

    public void restore(String filename, boolean enhanced) throws RemoteException;

    public void delete(String filename, boolean enhanced) throws RemoteException;

    public void reclaim(int diskSpace) throws RemoteException;

    public String state() throws RemoteException;

}