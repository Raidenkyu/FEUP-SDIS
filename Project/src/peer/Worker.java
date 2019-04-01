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

import java.security.MessageDigest;

import peer.Chunk;

public class Worker implements Runnable{
    public String task;
    public Object[] args;

    public Peer parent = null;
    int chunkSize;

    public Worker(String task, Object[] args, Peer peer) {
        this.task = task;

        this.args = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            this.args[i] = args[i];
        }

        this.parent = peer;
        this.chunkSize = Peer.chunkSize;
    }

    @Override
    public void run() {
        if (task.equals("backup")) {
            String filename = (String)args[0];
            int size = (Integer)args[1];
            int replicationDegree = (Integer) args[3];
            File file = new File(filename);
            byte[] data = new byte[(int) file.length()];
            FileInputStream in = new FileInputStream(file);
            System.out.println("uploading to server...");
            in.read(data, 0, data.length);

            backup(data, filename, size, replicationDegree);
            in.close();
        } else if (task.equals("delete")) {

        } else if (task.equals("restore")) {

        } else if (task.equals("reclaim")) {

        } else {
            System.out.println("Wrong task!");
            System.exit(-1);
        }
    }

    public void backup(byte[] data, String filename, int size, int replicationDegree) {
        byte[] buffer;
        int bytesRead;
        int numChunks = (int) Math.ceil((double) data.length / chunkSize);
        int senderId = Peer.id;
        String version = Peer.version;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(filename.getBytes());
        String fileId = new String(encodedhash);
        String CRLF = "\r\n";

        for (int i = 0; i < numChunks; i++) {
            int bufSize = data.length - i * chunkSize;
            if (bufSize > chunkSize)
                bufSize = chunkSize;

            buffer = new byte[bufSize];

            for (int j = 0; j < chunkSize; j++) {
                buffer[j] = data[i * chunkSize + j];
            }
            String body = new String(buffer);
            String msg = "PUTCHUNCK" + version + senderId + i + CRLF+ CRLF + body;
            System.out.println(msg);
            //peer.channels.get("MDB").send();
        }

        
        int stored = 0;

        while (stored < replicationDegree)
        {
            String response = Peer.channels.get("MC").messageQueue.poll();
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
}