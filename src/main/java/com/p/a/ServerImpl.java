package com.p.a;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;

public class ServerImpl extends UnicastRemoteObject implements ServerRemote {

	/**
	 * 
	 */
	private static final long serialVersionUID = 171404308282387361L;


	protected ServerImpl() throws RemoteException {
	}

	public Date getDate() throws RemoteException {
		return new Date();
	}

	
	public static void main(String[] args) {
		try {
			ServerRemote server = (ServerRemote)new ServerImpl();
			Naming.rebind("dateService", server);
		} catch (Exception e){
			System.out.println(e);
		}
	}
}
