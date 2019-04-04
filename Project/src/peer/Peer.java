package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;

import java.util.ArrayList;
import java.io.File;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Remote;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;

import java.util.Enumeration;

import peer.Chunk;

public class Peer implements PeerRMI
{
    static Peer instance;
    int id = 0;
    String version = "1.0";

    int chunkSize = 100;
    ArrayList<Chunk> chunks;

    ArrayList<String> peers;

    ThreadPoolExecutor pool;

    ConcurrentHashMap<String, PeerChannel> channels;

    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    public Peer(String id){
        this.id = Integer.parseInt(id);
        this.initPool();
        this.initChannels();
        this.initRMI();
        this.chunks = new ArrayList<Chunk>();
    }
    public static void main(String[] args)
    {
        instance = new Peer(args[0]);

    }


    public void backup(String filename, int replicationDegree)
    {
        Integer degree = replicationDegree;
        Object[] args = {filename,degree};
        Thread dataChannel = new Thread(new Worker("backup",args,instance),"Backup");
        pool.execute(dataChannel);
    }

    public void restore() {

    }

    public void delete() {

    }

    public void reclaim() {
        
    }

    public void state() {
        
    }

    public void initChannels(){
        PeerChannel MDB = new PeerChannel("MDB", "225.0.0.0",instance);
        PeerChannel MC = new PeerChannel("MC", "226.0.0.0",instance);
        PeerChannel MDR = new PeerChannel("MDR", "227.0.0.0",instance);
        Thread dataChannel = new Thread(MDB, "MDB");
        Thread controlChannel = new Thread(MC, "MC");
        Thread recoveryChannel = new Thread(MDR, "MDR");

        dataChannel.start();
        controlChannel.start();
        recoveryChannel.start();

        channels = new ConcurrentHashMap<String, PeerChannel>();

        channels.put(MDB.id, MDB);
        channels.put(MC.id, MC);
        channels.put(MDR.id, MDR);

    }

    public void initPool(){
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    }


    public ConcurrentHashMap<String, String> getMap(){
        return map;
    }

    private void initRMI(){
        try {
            PeerRMI stub = (PeerRMI) UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            String RMIName = "Peer" + this.id;
            registry.bind(RMIName, stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            // System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }


}