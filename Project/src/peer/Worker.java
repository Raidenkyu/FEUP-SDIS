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

public class Worker implements Runnable, PeerRMI
{
    public String task;
    public Object[] args;

    public Peer parent = null;

    public Worker(String task, Object[] args, Peer peer) 
    {
        this.task = task;

        this.args = new Object[args.length];
        for (int i = 0; i < args.length; i++)
        {
            this.args[i] = args[i];
        }

        this.parent = peer;
    }

    @Override
    public void run()
    {
        if (task.equals("backup"))
        {
            byte[] data = (byte[]) args[0];
            String filename = (String) args[1];
            Integer size = (Integer) args[2];
            Integer replicationDegree = (Integer) args[3];

            backup(data, filename, size, replicationDegree);
        }
        else if (task.equals("delete"))
        {

        }
        else if (task.equals("restore"))
        {

        }
        else if (task.equals("reclaim"))
        {
            
        }
        else
        {
            System.out.println("Wrong task!");
            System.exit(-1);
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

        int sent = 0;
        for (int i = 0; i < peers.size(); i++)
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
}