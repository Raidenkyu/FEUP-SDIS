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

import peer.Chunk;

public class Peer implements PeerRMI
{
    static int id = 0;
    static int port = 8080;
    static String multicastGroup = "225.0.0.0";
    static MulticastSocket socket;

    static int chunkSize = 100;
    static ArrayList<Chunk> chunks;

    static ArrayList<String> peers;

    static ThreadPoolExecutor pool;

    private static final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();


    public static void main(String[] args)
    {
        initChannels();
        initPool();
        try
        {
            chunks = new ArrayList<Chunk>();

            socket = new MulticastSocket(port);

            socket.setSoTimeout(2*1000); // 2 second timeout
            socket.joinGroup(InetAddress.getByName(multicastGroup));

            while (true) //Listen for peers
            {
                byte[] data = new byte[1000];
                DatagramPacket packet = new DatagramPacket(data, data.length);

                socket.receive(packet);

                String message = new String(packet.getData()).trim();


            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }



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

        for (int i = 0; i < peers.size() && i < replicationDegree; i++)
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
        Thread dataChannel = new Thread(new ChannelListener("MDB", "225.0.0.0"), "MDB");
        Thread controlChannel = new Thread(new ChannelListener("MC", "226.0.0.0"), "MC");
        Thread recoveryChannel = new Thread(new ChannelListener("MDR", "227.0.0.0"), "MDR");

        controlChannel.start();
    }

    public static void initPool(){
        pool = (ThreadPoolExecutor) Executors.newSingleThreadExecutor();
    }

}