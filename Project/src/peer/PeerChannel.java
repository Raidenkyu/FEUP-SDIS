package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.ThreadPoolExecutor;
import java.net.InetAddress;

public class PeerChannel implements Runnable {
    String id = "";
    MulticastSocket socket;
    int port = 8080;
    String multicastGroup = "";

    Peer parent = null;

    // Peer parent = Peer;

    public PeerChannel(String id, String multicastGroup, Peer peer) {
        this.id = id;
        this.multicastGroup = multicastGroup;
        this.parent = peer;
    }

    public void connect()
    {
        try{
            socket = new MulticastSocket(port);
            socket.setSoTimeout(2*1000); // 2 second timeout
            socket.joinGroup(InetAddress.getByName(multicastGroup));
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        this.connect();
        
        while (true) //Listen for peers
        {
            byte[] data = new byte[1000];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            socket.receive(packet);

            String message = new String(packet.getData()).trim();

            if (id.equals("MDB"))
            {
                this.MDBListener(message, packet.getSocketAddress());
            }
            else if (id.equals("MC"))
            {
                this.MCListener(message);
            }
            else if (id.equals("MDR"))
            {
                this.MDRListener(message);
            }
            else
            {
                System.err.println("Wrong PeerChannel ID!");
            }
        }

    }

    void MDBListener(String msg, InetAddress IP){
        String[] args = msg.split("/s+");

        if (args[0].equals("PUTCHUNK"))
        {
            String response = "";

            response += "STORED ";
            response += args[1] + " ";
            response += args[2] + " ";
            response += args[3] + " ";
            response += args[4] + " ";
            response += "\r\n\r\n";

            DatagramPacket packet = new DatagramPacket(port, )

            peer.channels.get("MC").send(response);

        }
            
    }

    void MCListener(String msg){


            
    }

    void MDRListener(String msg){


            
    }


    protected void send(String message, String IP)
    {
        socket
    }
    
}