package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import peer.Peer;

class Client {
    static boolean enhanced = false;
    static Peer stub = null;

    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Invalid Number of arguments");
        }

        locatePeer(args[0]);
        String operation = args[1];
        switch (operation) {
        case "BACKUP":
            if (args.length < 4) {
                System.out.println("Invalid Number of arguments");
            }
            int replicationDegree = Integer.parseInt(args[3]);
            backup(args[2], replicationDegree);
            break;
        case "RESTORE":
            if (args.length < 3) {
                System.out.println("Invalid Number of arguments");
            }
            restore(args[2]);
            break;
        case "DELETE":
            if (args.length < 3) {
                System.out.println("Invalid Number of arguments");
            }
            delete(args[2]);
            break;
        case "RECLAIM":
            if (args.length < 3) {
                System.out.println("Invalid Number of arguments");
            }
            int diskSpace = Integer.parseInt(args[2]);
            reclaim(diskSpace);
            break;
        case "STATE":
            if (args.length < 2) {
                System.out.println("Invalid Number of arguments");
            }
            state();
            break;
        default:
            System.out.println("Operation not recognised");
        }

    }

    static void backup(String filename, int replicationDegree) {

        
        
        

        stub.backup(filename, (int) file.length(), replicationDegree);
        in.close();
    }

    static void restore(String filename) {

    }

    static void delete(String filename) {

    }

    static void reclaim(int diskSpace) {

    }

    static void state() {

    }

    static void locatePeer(String peerName) {
        try {
            Registry registry = LocateRegistry.getRegistry(null);
            stub = (Peer) registry.lookup(peerName);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}