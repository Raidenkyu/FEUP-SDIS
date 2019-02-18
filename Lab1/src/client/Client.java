package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

	public static void main(String[] args) {
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		String msg = args[2];
		
		if(msg.equals("register")) {
			msg += ";" +  args[3] + ";" + args[4];
		}
		else if(msg.equals("lookup")){
			msg += ";" +  args[3];
		}
		else {
			System.out.println("Invalid Message");
		}
		DatagramSocket socket;
		
		byte[] data = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(data,data.length);
		try {
			socket = new DatagramSocket();
			socket.connect(InetAddress.getByName(host),port);
			socket.send(packet);
			byte[] replyData = new byte[128];
			DatagramPacket response = new DatagramPacket(replyData,replyData.length);
			socket.receive(response);
			String responseText = new String(response.getData());
			System.out.println(responseText);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		

	}

}
