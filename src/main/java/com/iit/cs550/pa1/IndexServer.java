package com.iit.cs550.pa1;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IndexServer extends Remote {

	public void  registry(String peerId, String filename) throws RemoteException;

	public List<String> search(String filename) throws RemoteException;

}
