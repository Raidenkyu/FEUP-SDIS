package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

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
        boolean enhanced = false;


        locatePeer(args[0]);
        String operation = args[1];
        if(operation.contains("ENH")){
            enhanced = true;
            operation = operation.substring(0, operation.length()-3);
        }
        System.out.println(operation);
        switch (operation) {
        case "BACKUP":
            if (args.length != 4) {
                System.out.println("Invalid Number of arguments");
                System.exit(1);
            }
            int replicationDegree = Integer.parseInt(args[3]);
            try{
            backup(args[2], replicationDegree, enhanced);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            break;
        case "RESTORE":
            if (args.length != 3) {
                System.out.println("Invalid Number of arguments");
                System.exit(1);
            }
            try{
                restore(args[2], enhanced);
            }
            catch(Exception e){
               e.printStackTrace();
            }
            
            break;
        case "DELETE":
            if (args.length < 3) {
                System.out.println("Invalid Number of arguments");
                System.exit(1);
            }
            
            try{
                delete(args[2], enhanced);
            }
            catch(Exception e){
               e.printStackTrace();
            }
            break;
        case "RECLAIM":
            if (args.length < 3) {
                System.out.println("Invalid Number of arguments");
                System.exit(1);
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
                System.exit(1);
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
            System.exit(1);
        }
        
        
        System.exit(0);
    }

    static void backup(String filename, int replicationDegree, boolean enhanced) {

        try{
            stub.backup(filename, replicationDegree, enhanced);
        }
        catch(RemoteException e){
           e.printStackTrace();
        }
        
    }

    static void restore(String filename, boolean enhanced) {
    	
    	try{
             stub.restore(filename, enhanced);
         }
         catch(RemoteException e){
            e.printStackTrace();
         }
    	
    }

    static void delete(String filename, boolean enhanced) {

        try {
            stub.delete(filename, enhanced);
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
    	    	
    	String info = null;
    	try{
    		info = stub.state();
        }
        catch(RemoteException e){
           e.printStackTrace();
        }
    	    	
    	System.out.println(info);
    }

    static void locatePeer(String accessPoint) {
        String RMIName = accessPoint;
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            stub = (PeerRMI) registry.lookup(RMIName);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}