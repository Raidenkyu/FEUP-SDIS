package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TreeMap;

public class Server{

	public static void main(String[] args) {
		
		System.out.println("Server is running");
		TreeMap<String,String> cars = new TreeMap<>();
		int port = Integer.parseInt(args[0]);
		int messageLength = 128;
		DatagramSocket socket;
		
		try {
			socket = new DatagramSocket(port);
			while(true) {
				
				byte[] data = new byte[messageLength];
				DatagramPacket packet = new DatagramPacket(data,messageLength);
				socket.receive(packet);
				String message = new String(packet.getData()), replyMsg;
				System.out.println(message);
				String[] parts = message.split(";");
				
				
				if(parts[0].equals("register")) {
					String returnValue = cars.put(parts[1],parts[2]);
					if(returnValue == null) {
						replyMsg = cars.size() + ";" + parts[1] + ";" +  parts[2];  
					}
					else {
						System.out.println("Plate already registered");
						replyMsg = -1 + ";" + parts[1] + ";" + parts[2];
					}
				}
				else if(parts[0].equals("lookup")){
					if(cars.get(parts[1]).equals(parts[2])) {
					replyMsg = cars.size() + ";" + parts[1] + ";" + parts[2];  
					}
					else {
						System.out.println("Plate not registered");
						replyMsg = -1 + ";" + parts[1] + ";" + parts[2];  
					}
				}
				else {
					System.out.println("Invalid Message");
					replyMsg = -1 + ";" + parts[1] + ";" + parts[2];  
				}
				
				byte[] replyData = replyMsg.getBytes();
				DatagramPacket reply = new DatagramPacket(replyData,replyData.length, packet.getAddress(), packet.getPort());
				socket.send(reply);
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		

	}

}
