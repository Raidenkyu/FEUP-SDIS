package peer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.io.FileInputStream;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;

import peer.Chunk;

public class Peer implements PeerRMI {
    static Peer instance;
    int id = 0;
    String version = "1.0";
    String accessPoint;

    public static final String CRLF = "\r\n";
    public static final boolean DEBUG = true;

    public static int chunkSize = 64000;

    public PeerStorage storage;

    protected ArrayList<Pair<String, Chunk>> backedChunks;

    ThreadPoolExecutor pool;

    ConcurrentHashMap<String, PeerChannel> channels;
    
    TCPChannel tcp;
    String chunkPath = Paths.get(System.getProperty("user.dir")).toString();

    public Peer(String version, String id, String accessPoint) {
        this.storage = new PeerStorage(this);
        backedChunks = new ArrayList<Pair<String, Chunk>>();
        this.version = version;
        this.id = Integer.parseInt(id);
        this.accessPoint = accessPoint;
        this.initPool();
        this.initChannels();
        this.initRMI();
        this.retrieveChunksFromFiles();

        if (this.enhanced())
            this.checkDeletedChunks(); // DELETE Enhancement

    }

    public Peer(String version, String id, String accessPoint,String MCIP, int MCPort, String MDBIP, int MDBPort, String MDRIP, int MDRPort) {
        this.storage = new PeerStorage(this);
        backedChunks = new ArrayList<Pair<String, Chunk>>();
        this.version = version;
        this.id = Integer.parseInt(id);
        this.accessPoint = accessPoint;
        this.initPool();
        this.initChannels(MCIP, MCPort, MDBIP, MDBPort, MDRIP, MDRPort);
        this.initRMI();
        this.retrieveChunksFromFiles();

        if (this.enhanced())
            this.checkDeletedChunks(); // DELETE Enhancement

    }

    public static void main(String[] args)
    {
        if(args.length == 3){
            instance = new Peer(args[0],args[1],args[2]);
        }
        else if(args.length == 9){
            instance = new Peer(args[0],args[1],args[2],args[3],Integer.parseInt(args[4]),args[5],Integer.parseInt(args[6]),args[7],Integer.parseInt(args[8]));
        }
        else{
            System.out.println("Error: Invalid number of arguments");
        }
    }

    public void backup(String fileId, int replicationDegree, boolean enhanced) {
        if (enhanced && (this.version.equals("1.0"))) {
            System.out.println("Error: The current protocol version does not support enhanced implementations");
            return;
        }
        Integer degree = replicationDegree;
        Object[] args = { fileId, degree };
        Thread backupThread = new Thread(new Worker("backup", args, this, enhanced), "Backup");
        pool.execute(backupThread);
    }

    public void restore(String filename, boolean enhanced) {
        if (enhanced && (this.version.equals("1.0"))) {
            System.out.println("Error: The current proctocol version does not support enhanced implementations");
            return;
        }
        Object[] args = { filename };
        Thread restoreThread = new Thread(new Worker("restore", args, this, enhanced), "Restore");
        pool.execute(restoreThread);
    }

    public void delete(String filename, boolean enhanced) {
        if (enhanced && (this.version.equals("1.0"))) {
            System.out.println("Error: The current protocol version does not support enhanced implementations");
            return;
        }
        Object[] args = { filename };
        Thread deleteThread = new Thread(new Worker("delete", args, this, enhanced), "Delete");
        pool.execute(deleteThread);
    }

    public void reclaim(int DiskSpace) {
        Object[] args = { DiskSpace };
        Thread backupThread = new Thread(new Worker("reclaim", args, this), "Reclaim");
        pool.execute(backupThread);
    }

    public String state() {

        String info = "";

        if (backedChunks.size() == 0)
            info += "No files were backed up from this peer\n";
        else {
            info += "This peer backed up the following files:\n";

            HashSet<String> filenames = new HashSet<String>();
            Chunk chunk;
            for (int i = 0; i < backedChunks.size(); i++) 
            {                
                if (filenames.add(backedChunks.get(i).first)) // Start of a different file
                {
                	chunk = backedChunks.get(i).second;
                	
                    info += "\tFile: pathname = " + backedChunks.get(i).first + ", id = " + chunk.fileId + ", desired replication degree = " + chunk.desiredReplicationDegree + "\n";
                    
                    for (int j = 0; j < backedChunks.size(); j++)
                    {                    	
                    	if (backedChunks.get(j).first.equals(backedChunks.get(i).first))
                    	{
                            chunk = backedChunks.get(j).second;
                        	info += "\t\tChunk: id = " + chunk.index + ", perceived replication degree = " + chunk.getActualReplicaitonDegree() + "\n";
                    	}
                    }
                }
              
            }
        }

        Set<Map.Entry<String, Chunk>> chunks = storage.getChunks().entrySet();

        if (chunks.size() == 0)
            info += "No chunks are stored on this peer\n";
        else {
            info += "This peer has the following chunks:\n";

            Chunk chunk;
            for (Map.Entry<String, Chunk> entry : chunks) {
                chunk = entry.getValue();
                info += "\tChunk: id = " + chunk.index + ", size = " + chunk.data.length / 1000
                        + "KB, perceived replication degree = " + chunk.getActualReplicaitonDegree() + "\n";
            }
        }

        info += "This peer has a maximum storage of " + storage.getCapacity() / 1000 + "KB and is currently using "
                + storage.getUsedSpace() / 1000 + "KB\n";

        return info;
    }

    public void chunkBackup(Chunk chunk) {
        Object[] args = { chunk };
        Thread chunkBackupThread = new Thread(new Worker("chunkBackup", args, this), "ChunkBackup");
        pool.execute(chunkBackupThread);
    }

    public void initChannels() {

        PeerChannel MC = new PeerChannel("MC", "226.0.0.0", this);
        PeerChannel MDB = new PeerChannel("MDB", "225.0.0.0", this);
        PeerChannel MDR = new PeerChannel("MDR", "227.0.0.0", this);
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

    public void initChannels(String MCIP, int MCPort, String MDBIP, int MDBPort, String MDRIP, int MDRPort) {

        PeerChannel MC = new PeerChannel("MC", MCIP, MCPort, this);
        PeerChannel MDB = new PeerChannel("MDB", MDBIP, MDBPort, this);
        PeerChannel MDR = new PeerChannel("MDR", MDRIP, MDRPort, this);
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

    public void initPool() {
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    }

    private void initRMI() {
        try {
            PeerRMI stub = (PeerRMI) UnicastRemoteObject.exportObject(this, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(this.accessPoint, stub);

            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Failed to connect to RMI!");
            System.exit(1);
        }
    }

    private void retrieveChunksFromFiles() {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(chunkPath))) {
            for (Path child : dirStream) {

                String pathStr = child.toString();
                int pos = pathStr.lastIndexOf(File.separator);
                if (pos != -1)
                    pathStr = pathStr.substring(pos + 1);

                if (Files.isDirectory(child) && pathStr.matches("peer" + id)) {
                    try (DirectoryStream<Path> dirStream2 = Files.newDirectoryStream(child)) {
                        for (Path child2 : dirStream2) {

                            pathStr = child2.toString();
                            pos = pathStr.lastIndexOf(File.separator);
                            if (pos != -1)
                                pathStr = pathStr.substring(pos + 1);

                            if (Files.isDirectory(child2) && pathStr.matches("backup")) {

                                try (DirectoryStream<Path> dirStream3 = Files.newDirectoryStream(child2)) {
                                    for (Path child3 : dirStream3) {

                                        pathStr = child3.toString();
                                        pos = pathStr.lastIndexOf(File.separator);
                                        if (pos != -1)
                                            pathStr = pathStr.substring(pos + 1);

                                        if (Files.isDirectory(child3)) {
                                            try (DirectoryStream<Path> dirStream4 = Files.newDirectoryStream(child3)) {
                                                for (Path child4 : dirStream4) {
                                                    pathStr = child4.toString();
                                                    pos = pathStr.lastIndexOf(File.separator);
                                                    if (pos != -1)
                                                        pathStr = pathStr.substring(pos + 1);

                                                    if (pathStr.matches("chk[0-9]+")) {

                                                        try {
                                                            File file = new File(child4.toString());
                                                            FileInputStream in = new FileInputStream(file);
                                                            ObjectInputStream ois = new ObjectInputStream(in);

                                                            Chunk chunk = null;

                                                            try {
                                                                chunk = (Chunk) ois.readObject();
                                                            } catch (ClassNotFoundException e) {
                                                                e.printStackTrace();
                                                            }

                                                            ois.close();

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
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkDeletedChunks() // Sends a GETINITIATOR message for every unique fileId loaded from files and
                                      // expects an INITIATOR message as response from the initiator peer of the
                                      // backup of that file
    {
        Chunk chunk;
        byte[] msg;
        Collection<Chunk> chunks = storage.getChunks().values();
        HashSet<String> missingFileIds = new HashSet<String>();

        for (Iterator<Chunk> it = chunks.iterator(); it.hasNext();) {
            chunk = it.next();

            if (missingFileIds.add(chunk.fileId)) // Only one message for each distinct FileId
            {
                String msgString = makeHeader("GETINITIATOR", chunk);
                msg = msgString.getBytes();

                channels.get("MC").send(msg);
            }
        }

        Timer timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {

                for (Iterator<byte[]> it = channels.get("MC").messageQueue.iterator(); it.hasNext();) {
                    byte[] msg = it.next();
                    String[] args = parseHeader(msg).split(" +");

                    String fileId = args[3];

                    if (args[0].equals("INITIATOR") && missingFileIds.remove(fileId))
                        it.remove();
                }

                Chunk currChunk;

                for (Iterator<Chunk> it = chunks.iterator(); it.hasNext();) {
                    currChunk = it.next();

                    if (missingFileIds.contains(currChunk.fileId)) {
                        System.out.println("Removed deleted chunk " + currChunk);

                        currChunk.delete(chunkPath.toString(), id);
                        storage.deleteChunk(currChunk.key());
                    }
                }

                this.cancel();
            }
        }, 1000);
    }

    protected byte[] makeMsg(String header, Chunk chunk) {
        byte[] headerBytes = header.getBytes();
        byte[] chunkBytes = chunk.data;
        byte[] msg = new byte[headerBytes.length + chunkBytes.length];
        System.arraycopy(headerBytes, 0, msg, 0, headerBytes.length);
        System.arraycopy(chunkBytes, 0, msg, headerBytes.length, chunkBytes.length);

        return msg;
    }

    protected String makeHeader(String op, Chunk chunk) {
        String header = "";
        header += op;
        header += " " + this.version;
        header += " " + this.id;
        header += " " + chunk.fileId;

        if (op.equals("PUTCHUNK") || op.equals("STORED") || op.equals("GETCHUNK") || op.equals("CHUNK")
                || op.equals("REMOVED") || op.equals("REMOVE"))
            header += " " + chunk.index;

        if (op.equals("PUTCHUNK"))
            header += " " + chunk.desiredReplicationDegree;

        header += " " + CRLF + CRLF;

        return header;
    }

    protected String parseHeader(byte[] packetData) {
        String header = "";
        int i;
        for (i = 0; i < packetData.length - 1; i++) {
            if (packetData[i] == '\r' && packetData[i + 1] == '\n')
                break;

            header += (char) packetData[i];
        }

        if (i == packetData.length - 1)
            return null;

        return header;
    }

    protected byte[] parseChunk(byte[] packetData) {
        String msg = new String(packetData);
        int dataIndex = msg.indexOf(CRLF);
        dataIndex += 2 * CRLF.length();
        byte[] chunkData = Arrays.copyOfRange(packetData, dataIndex, packetData.length);
        return chunkData;
    }

    public void launchTCPServer() {
        TCPChannel tcpChannel = new TCPChannel(this);

        Thread TCPThread = new Thread(tcpChannel, "MDR");

        TCPThread.start();
        this.tcp = tcpChannel;
    }

    public boolean enhanced() {
        return (!version.equals("1.0"));
    }
}