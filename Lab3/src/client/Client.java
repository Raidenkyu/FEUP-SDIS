package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import server.ServerRegistry;;

public class Client {

	public static void main(String[] args) {
		String op = args[0];
		String plate = args[1];
		String response = "";

		try {
			Registry registry = LocateRegistry.getRegistry("10.227.161.159");
			ServerRegistry stub = (ServerRegistry) registry.lookup("ServerRegistry");
			if (op.equals("register")) {
				String owner = args[2];
				response = stub.register(plate,owner);
			} else if (op.equals("lookup")) {
				response = stub.lookup(plate);
			} else {
				System.out.println("Invalid Message");
			}
			System.out.println("response: " + response);
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}

	}

}
