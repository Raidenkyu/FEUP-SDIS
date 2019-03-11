package server;
        
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Remote;

import java.util.TreeMap;
        
public class Server implements ServerRegistry {
	TreeMap<String,String> cars;
        
    public Server() {
		this.cars = new TreeMap<>();
	}

        
    public static void main(String args[]) {
        
        try {
            Server obj = (Server)new Server();
            ServerRegistry stub = (ServerRegistry) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("ServerRegistry", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            // System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
	}
	
	public String lookup(String plate){
		String owner = this.cars.get(plate);
		String replyMsg = "";
		if(owner != null) {
			replyMsg = this.cars.size() + ";" + plate + ";" + owner;  
		}
		else {
				System.out.println("Plate not registered");
				replyMsg = -1 + ";" + plate;  
		}
		return replyMsg;
	}

	public String register(String plate, String owner){
		String returnValue = this.cars.put(plate,owner);
		String replyMsg = "";
		if(returnValue == null) {
			replyMsg = this.cars.size() + ";" + plate + ";" +  owner;  
		}
		else {
			System.out.println("Plate already registered");
			replyMsg = -1 + ";" +plate + ";" + owner;
		}

		return replyMsg;
	}
}