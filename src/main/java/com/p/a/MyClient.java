package com.p.a;

import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;

public class MyClient {
	
	public static void main(String[] args) {
		new MyClient("localhost");
	}
	
	public MyClient(String hostName) {
		try {
			ServerRemote dateServer = (ServerRemote) Naming.lookup(
					"rmi://" + hostName + "/dateService");
			System.out.println(dateServer.getDate());
			
		} catch (IOException e) {
			//bad URL
			System.out.print(e);
		} catch (NotBoundException e) {
			// remote object not bound
			System.out.println(e);
		}
	}
}
