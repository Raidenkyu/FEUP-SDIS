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
    static Peer instance = new Peer();
    static int id = 0;
    static String version = "1.0";

    static int chunkSize = 100;
    static ArrayList<Chunk> chunks;

    static ArrayList<String> peers;

    static ThreadPoolExecutor pool;

    static ConcurrentHashMap<String, PeerChannel> channels;

    private static final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();


    public static void main(String[] args)
    {
        initPool();
        initChannels();

        chunks = new ArrayList<Chunk>();
    }


    public void backup(String filename, int replicationDegree)
    {
        Object[] args = {filename,replicationDegree};
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

    public static void initChannels(){
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

    public static void initPool(){
        pool = (ThreadPoolExecutor) Executors.newSingleThreadExecutor();
    }


    public static ConcurrentHashMap<String, String> getMap(){
        return map;
    }


}