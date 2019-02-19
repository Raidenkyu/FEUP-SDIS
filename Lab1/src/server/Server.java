package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TreeMap;

public class Server{

	static int bufferSize = 256;
	static DatagramSocket socket;
	static TreeMap<String,String> cars = new TreeMap<String,String>();
	
	private static boolean validPlate(String plate)
	{
		int numberCounter = 0, letterCounter = 0, dashCounter = 0;
		
		
		for (int i = 0; i < plate.length(); i++) {
			
			if (plate.charAt(i) >= 'A' && plate.charAt(i) <= 'Z')
				letterCounter++;
			
			else if (plate.charAt(i) >= '0' && plate.charAt(i) <= '9')
				numberCounter++;
			
			else if (plate.charAt(i) == '-')
				dashCounter++;
			
			else
				return false;
			
		}
		
		return (numberCounter == 4 && letterCounter == 2 && dashCounter == 2);
	}
	
	public static void main(String[] args) {
		
		System.out.println("Server is running\n");
		
		int port = Integer.parseInt(args[0]);
		
		try {
			socket = new DatagramSocket(port);
			
			while(true) {
				
				byte[] data = new byte[bufferSize];
				DatagramPacket receivedPacket = new DatagramPacket(data, data.length);
				
				socket.receive(receivedPacket);
				InetAddress senderIP = receivedPacket.getAddress();
				String message = new String(receivedPacket.getData()).trim();
				
				String[] parts = message.split(";");
				String printMessage = "";
				
				if (!validPlate(parts[1])) {
					
					System.err.println("Invalid plate");
				}
					
				
				for (int i = 0; i < parts.length; i++)
					printMessage += parts[i] + " ";
				
				System.out.println(printMessage);
				
				if (parts[0].equals("register")) {
					
					String returnValue = cars.put(parts[1],parts[2]);
					
					if (returnValue == null) {
						message = cars.size() + ";" + parts[1] + ";" +  parts[2];  
					}
					else {
						System.out.println("Plate already registered");
						message = -1 + ";" + parts[1] + ";" + parts[2];
					}
					
				}			
				else if (parts[0].equals("lookup")) {
					
					String name = cars.get(parts[1]);					
					
					if (name != null) {
						message = cars.size() + ";" + parts[1] + ";" + name;  
					}
					else {
						System.out.println("Plate not registered");
						message = -1 + ";" + parts[1] + "ERROR";  
					}
					
				}
				else {
					System.out.println("Invalid Message");
					message = -1 + ";" + parts[1] + ";" + parts[2];  
				}
				
				data = message.getBytes();
				DatagramPacket responsePacket = new DatagramPacket(data, data.length, receivedPacket.getAddress(), receivedPacket.getPort());
				
				socket.send(responsePacket);
				
				System.out.println("");
			}
			
			
		} catch (IOException e) {
			System.err.println("Failed to open server!");
		}
		
	}

}
