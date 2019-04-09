package peer;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.io.FileInputStream;

import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;

import javafx.util.Pair;

import peer.Chunk;

public class Peer implements PeerRMI
{
    static Peer instance;
    int id = 0;
    String version = "1.0";
    
    public static final String CRLF = "\r\n";

    public static int chunkSize = 64000;
    
    public PeerStorage storage;
    
    protected ArrayList<Pair<String, Chunk>> backedChunks;

    ArrayList<String> peers;

    ThreadPoolExecutor pool;

    ConcurrentHashMap<String, PeerChannel> channels;

    String chunkPath = Paths.get(System.getProperty("user.dir")).toString();

    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    public Peer(String version, String id){
        this.storage = new PeerStorage(this);
        backedChunks = new ArrayList<Pair<String, Chunk>>();
        this.version = version;
		this.id = Integer.parseInt(id);
		// retrieveChunksFromFiles(); NOTE : Not specified in protocol

		this.initPool();
		this.initChannels();
		this.initRMI();

    }

    public static void main(String[] args)
    {
        instance = new Peer(args[0],args[1]);
    }


    public void backup(String fileId, int replicationDegree) {
        Integer degree = replicationDegree;
        Object[] args = {fileId,degree};
        Thread backupThread = new Thread(new Worker("backup", args, this), "Backup");
        pool.execute(backupThread);
    }

    public void restore(String filename) {
        Object[] args = {filename};
        Thread restoreThread = new Thread(new Worker("restore", args, this), "Restore");
        pool.execute(restoreThread);
    }

    public void delete(String filename) {
        Object[] args = { filename };
        Thread deleteThread = new Thread(new Worker("delete", args, this), "Delete");
        pool.execute(deleteThread);
    }

    public void reclaim(int DiskSpace) {
        Object[] args = {DiskSpace};
        Thread backupThread = new Thread(new Worker("backup",args,this),"Backup");
        pool.execute(backupThread);
    }

    public String state() {

    	String info = "";
    	
    	if (backedChunks.size() == 0)
    		info += "No files were backed up from this peer\n";
    	else
    	{
    		info += "This peer backed up the following files:\n";
    		
    		Chunk chunk;
        	for (int i = 0; i < backedChunks.size(); i++)
        	{
        		chunk = backedChunks.get(i).getValue();
        		if (chunk.index == 0) // Start of a different file
        		{
            		info += "\tFile: pathname = " + backedChunks.get(i).getKey() + ", id = " + chunk.fileId + ", desired replication degree = " + chunk.desiredReplicationDegree + "\n";
        		}
        		info += "\t\tChunk: id = " + chunk.index + ", perceived replication degree = " + chunk.getActualReplicaitonDegree() + "\n";
        	}
    	}
    	
    	Set<Map.Entry<String,Chunk>> chunks = storage.getChunks().entrySet();
    	
    	if (chunks.size() == 0)
    		info += "No chunks are stored on this peer\n";
    	else
    	{
    		info += "This peer has the following chunks:\n";

    		Chunk chunk;
        	for (Map.Entry<String, Chunk> entry : chunks)
        	{
        		chunk = entry.getValue();
        		info += "\tChunk: id = " + chunk.index + ", size = " + chunk.data.length/1000 + "KB, perceived replication degree = " + chunk.getActualReplicaitonDegree() + "\n";
        	}
    	}
    	
    	info += "This peer has a maximum storage of " + storage.getCapacity()/1000 + "KB and is currently using " + storage.getUsedSpace()/1000 + "KB\n";

    	return info;
    }

    public void chunkBackup(Chunk chunk){
        Object[] args = {chunk};
        Thread chunkBackupThread = new Thread(new Worker("chunkBackup", args, this), "chunkBackup");
        pool.execute(chunkBackupThread);
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

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Failed to connect to RMI!");
            System.exit(1);
        }
    }


    private void retrieveChunksFromFiles()
    {
        String fileId;
        int chunkIndex;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(chunkPath))) {
            for (Path child : dirStream) {
                
                String pathStr = child.toString();                
                int pos = pathStr.lastIndexOf(File.separator);
                if (pos != -1)
                	pathStr = pathStr.substring(pos+1);

                if (Files.isDirectory(child) && pathStr.matches("peer"+id))
                {
                    try (DirectoryStream<Path> dirStream2 = Files.newDirectoryStream(child)) {
                        for (Path child2 : dirStream2) {

                            pathStr = child2.toString();
                            pos = pathStr.lastIndexOf(File.separator);
                            if (pos != -1)
                            	pathStr = pathStr.substring(pos+1);

                            if (Files.isDirectory(child2) && pathStr.matches("backup")) 
                            {

                                try (DirectoryStream<Path> dirStream3 = Files.newDirectoryStream(child2)) {
                                    for (Path child3 : dirStream3) {

                                        pathStr = child3.toString();
                                        pos = pathStr.lastIndexOf(File.separator);
                                        if (pos != -1)
                                        	pathStr = pathStr.substring(pos+1);

                                        fileId = pathStr;

                                        if (Files.isDirectory(child3)) 
                                        {
                                            try (DirectoryStream<Path> dirStream4 = Files.newDirectoryStream(child3)) {
                                                for (Path child4 : dirStream4) {
                                                    pathStr = child4.toString();
                                                    pos = pathStr.lastIndexOf(File.separator);
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
                                                            this.storage.addChunk(chunk);

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
    
    
    protected byte[] makeMsg(String header, Chunk chunk) {
        byte[] headerBytes = header.getBytes();
        byte[] chunkBytes = chunk.data;
        byte[] msg = new byte[headerBytes.length + chunkBytes.length];
        System.arraycopy(headerBytes, 0, msg, 0, headerBytes.length);
        System.arraycopy(chunkBytes, 0, msg, headerBytes.length, chunkBytes.length);

        return msg;
    }

    protected String makeHeader(String op, Chunk chunk){
        String header = "";
        header += op;
		header += " " + this.version; 
		header += " " + this.id;
		header += " " + chunk.fileId;
		
		if (op.equals("PUTCHUNK") || op.equals("STORED") || op.equals("GETCHUNK") || op.equals("CHUNK") || op.equals("REMOVED"))
			header += " " + chunk.index;
		
		if (op.equals("PUTCHUNK"))
			header += " " + chunk.desiredReplicationDegree;
		
        header += " " + CRLF + CRLF;
                        
        return header;
    }
    
    protected String parseHeader(byte[] packetData){
        String header = "";
        int i;
        for (i = 0; i < packetData.length-1; i++) 
        {
        	if (packetData[i] == '\r' && packetData[i+1] == '\n')
        		break;
        	
        	header += (char)packetData[i];
        }
        
        if (i == packetData.length-1)
        	return null;
                
        return header;
    }

    protected byte[] parseChunk(byte[] packetData){
        String msg = new String(packetData);
        int dataIndex = msg.indexOf(CRLF);
        dataIndex += 2*CRLF.length();
        byte[] chunkData = Arrays.copyOfRange(packetData, dataIndex, packetData.length);
        return chunkData;
    }
}