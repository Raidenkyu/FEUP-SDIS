package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;
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

import peer.Chunk;

public class Peer implements PeerRMI
{
    static Peer instance = new Peer();
    static int id = 0;

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


    public void backup(byte[] data, String filename, int size, int replicationDegree)
    {
        byte[] buffer;
        int bytesRead;
        int numChunks = (int)Math.ceil((double)data.length/chunkSize);

        for (int i = 0; i < numChunks; i++)
        {
            int bufSize = data.length - i*chunkSize;
            if (bufSize > chunkSize)
                bufSize = chunkSize;

            buffer = new byte[bufSize];

            for (int j = 0; j < chunkSize; j++)
            {
                buffer[j] = data[i*chunkSize + j];
            }

            chunks.add(new Chunk(buffer, i, numChunks, "id"));
        }

        int sent = 0;
        for (int i = 0; i < peers.size() && sent < replicationDegree; i++)
        {
            // Send chunk to peers

            
        }
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
        Thread dataChannel = new Thread(new PeerChannel("MDB", "225.0.0.0",instance), "MDB");
        Thread controlChannel = new Thread(new PeerChannel("MC", "226.0.0.0",instance), "MC");
        Thread recoveryChannel = new Thread(new PeerChannel("MDR", "227.0.0.0",instance), "MDR");

        dataChannel.start();
        controlChannel.start();
        recoveryChannel.start();

        channels = new ConcurrentHashMap<String, String>();

        channels.put(dataChannel.id, dataChannel);
        channels.put(controlChannel.id, controlChannel);
        channels.put(recoveryChannel.id, recoveryChannel);

    }

    public static void initPool(){
        pool = (ThreadPoolExecutor) Executors.newSingleThreadExecutor();
    }


    public static ConcurrentHashMap<String, String> getMap(){
        return map;
    }

}