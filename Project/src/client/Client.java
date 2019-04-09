package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import javafx.util.Pair;
import peer.Chunk;
import peer.PeerRMI;
import java.rmi.RemoteException;

class Client {
    static boolean enhanced = false;
    static PeerRMI stub = null;

    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Invalid Number of arguments");
            System.exit(1);
        }

        locatePeer(args[0]);
        String operation = args[1];
        
        switch (operation) {
        case "BACKUP":
            if (args.length != 4) {
                System.out.println("Invalid Number of arguments");
            }
            int replicationDegree = Integer.parseInt(args[3]);
            try{
            backup(args[2], replicationDegree);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            break;
        case "RESTORE":
            if (args.length != 3) {
                System.out.println("Invalid Number of arguments");
            }
            try{
                restore(args[2]);
            }
            catch(Exception e){
               e.printStackTrace();
            }
            
            break;
        case "DELETE":
            if (args.length < 3) {
                System.out.println("Invalid Number of arguments");
            }
            
            try{
                delete(args[2]);
            }
            catch(Exception e){
               e.printStackTrace();
            }
            break;
        case "RECLAIM":
            if (args.length < 3) {
                System.out.println("Invalid Number of arguments");
            }
            int diskSpace = Integer.parseInt(args[2]);
            
            try{
                reclaim(diskSpace);
            }
            catch(Exception e){
               e.printStackTrace();
            }
            break;
        case "STATE":
            if (args.length < 2) {
                System.out.println("Invalid Number of arguments");
            }
            try{
                state();
            }
            catch(Exception e){
               e.printStackTrace();
            }
            
            break;
        default:
            System.out.println("Operation not recognised");
        }

    }

    static void backup(String filename, int replicationDegree) {

        try{
            stub.backup(filename, replicationDegree);
        }
        catch(RemoteException e){
           e.printStackTrace();
        }
        
    }

    static void restore(String filename) {
    	
    	try{
             stub.restore(filename);
         }
         catch(RemoteException e){
            e.printStackTrace();
         }
    	
    }

    static void delete(String filename) {

        try {
            stub.delete(filename);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    static void reclaim(int diskSpace) {
        try{
            stub.reclaim(diskSpace);
        }
        catch(RemoteException e){
           e.printStackTrace();
        }
        
    }

    static void state() {
    	
    	long start = System.currentTimeMillis();
    	
    	String info = null;
    	try{
    		info = stub.state();
        }
        catch(RemoteException e){
           e.printStackTrace();
        }
    	
    	long deltaTime = System.currentTimeMillis() - start;
    	
    	System.out.println(info);
    }

    static void locatePeer(String peerName) {
        String RMIName = "Peer" + peerName;
        try {
            Registry registry = LocateRegistry.getRegistry(null);
            stub = (PeerRMI) registry.lookup(RMIName);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}