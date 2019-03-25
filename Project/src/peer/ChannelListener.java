package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;

public class ChannelListener implements Runnable
{
    String id = "";
    MulticastSocket socket;
    int port = 8080;
    String multicastGroup = "";

    // Peer parent = Peer;

    public ChannelListener(String id, String multicastGroupt)
    {
        this.id = id;
        this.multicastGroup = multicastGroup;
    }


    @Override
    public void run()
    {
        socket = new MulticastSocket(port);

        socket.setSoTimeout(2*1000); // 2 second timeout
        socket.joinGroup(InetAddress.getByName(multicastGroup));

        while (true) //Listen for peers
        {
            byte[] data = new byte[1000];
            DatagramPacket packet = new DatagramPacket(data, data.length);

            socket.receive(packet);

            String message = new String(packet.getData()).trim();

            // Handle message
        }
    }
}