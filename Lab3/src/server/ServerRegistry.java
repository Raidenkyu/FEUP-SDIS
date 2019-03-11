package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerRegistry extends Remote {
	

	public String lookup(String plate) throws RemoteException;

	public String register(String plate, String owner) throws RemoteException;

}
