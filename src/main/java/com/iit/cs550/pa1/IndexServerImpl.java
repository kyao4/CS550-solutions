package com.iit.cs550.pa1;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.iit.cs550.pa1.IndexServer;

public class IndexServerImpl extends UnicastRemoteObject implements IndexServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	static Logger logger = LogManager.getLogger(IndexServerImpl.class);
	
	Map<String, List<String>> registry = new Hashtable<String, List<String>>(); 	
	
	protected IndexServerImpl() throws RemoteException {
		super();
	}
	
	public void  registry(String peerId, String filename) throws RemoteException {
		logger.info("called registry with peerId: {}, filename:{}", peerId, filename);
		if (this.registry.containsKey(filename)) {
			List<String> peerList = this.registry.get(filename);
			peerList.add(peerId);
		} else {
			List<String> peerList = new ArrayList<String>();
			peerList.add(peerId);
			registry.put(filename, peerList);
		}
	}
	
	public List<String> search(String filename) throws RemoteException {
		logger.info("called search with filename: {}", filename);
		return this.registry.get(filename);
	}
	
	public static void main(String[] args) {
		try {
			IndexServer server = new IndexServerImpl();
			Naming.rebind("indexService", server);
			logger.info("registered remote object IndexServer with name indexService on RMIRegistry.");
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
