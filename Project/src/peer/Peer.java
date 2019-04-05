package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;

import java.util.ArrayList;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    Path chunkPath = Paths.get("");

    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    public Peer(String id){

		this.chunks = new ArrayList<Chunk>();
		this.id = Integer.parseInt(id);
		retrieveChunksFromFiles();

		this.initPool();
		this.initChannels();
		this.initRMI();

        }

    public static void main(String[] args)
    {
        instance = new Peer(args[0]);
    }


    public void backup(String filename, int replicationDegree)
    {
        Integer degree = replicationDegree;
        Object[] args = {filename,degree};
        Thread dataChannel = new Thread(new Worker("backup",args,this),"Backup");
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
        PeerChannel MDB = new PeerChannel("MDB", "225.0.0.0",this);
        PeerChannel MC = new PeerChannel("MC", "226.0.0.0",this);
        PeerChannel MDR = new PeerChannel("MDR", "227.0.0.0",this);
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


    private void retrieveChunksFromFiles()
    {
        String fileId;
        int chunkIndex;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(chunkPath)) {
            for (Path child : dirStream) {
                
                String pathStr = child.toString();                
                int pos = pathStr.lastIndexOf('\\');
                if (pos != -1)
                	pathStr = pathStr.substring(pos+1);

                if (Files.isDirectory(child) && pathStr.matches("peer"+id))
                {
                    try (DirectoryStream<Path> dirStream2 = Files.newDirectoryStream(child)) {
                        for (Path child2 : dirStream2) {

                            pathStr = child2.toString();
                            pos = pathStr.lastIndexOf('\\');
                            if (pos != -1)
                            	pathStr = pathStr.substring(pos+1);

                            if (Files.isDirectory(child2) && pathStr.matches("backup")) 
                            {

                                try (DirectoryStream<Path> dirStream3 = Files.newDirectoryStream(child2)) {
                                    for (Path child3 : dirStream3) {

                                        pathStr = child3.toString();
                                        pos = pathStr.lastIndexOf('\\');
                                        if (pos != -1)
                                        	pathStr = pathStr.substring(pos+1);

                                        fileId = pathStr;

                                        if (Files.isDirectory(child3)) 
                                        {
                                            try (DirectoryStream<Path> dirStream4 = Files.newDirectoryStream(child3)) {
                                                for (Path child4 : dirStream4) {
                                                    pathStr = child4.toString();
                                                    pos = pathStr.lastIndexOf('\\');
                                                    if (pos != -1)
                                                    	pathStr = pathStr.substring(pos+1);

                                                    if (pathStr.matches("chk[0-9]+")) {

                                                        chunkIndex = Integer.parseInt(pathStr.substring(3));

                                                        try {
                                                            File file = new File(child4.toString());
                                                            FileInputStream in = new FileInputStream(file);
                                                            byte[] data = new byte[(int) file.length()];
                                                            in.read(data, 0, data.length);
                                                            in.close();
                                                            
                                                            Chunk chunk = new Chunk(data, chunkIndex, fileId, 1);
                                                            
                                                            chunks.add(chunk);

                                                            System.out.println("Added chunk: " + chunk);

                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

//                System.out.println(child);
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void storeChunk(Chunk chunk)
    {

        // try (FileOutputStream stream = new FileOutputStream(path)) {
        //     stream.write(bytes);
        // }
    }
}