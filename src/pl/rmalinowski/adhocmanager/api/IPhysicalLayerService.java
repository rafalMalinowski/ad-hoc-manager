package pl.rmalinowski.adhocmanager.api;

import java.util.Set;

import pl.rmalinowski.adhocmanager.model.Node;
import pl.rmalinowski.adhocmanager.model.packets.Packet;

public interface IPhysicalLayerService {

	public void initialize();
	
	public void sendPacket(Packet packet, String destination);
	
	public void sendPacketBroadcast(Packet packet);
	
	public void searchForNeighbours();
	
	public void cancelSearchingForNeighbours();

	public void connectToNeighbours();
	
	public Set<Node> getConnectedDevices();
	
	public String getLocalAddress();
	
	public void sendPacketBroadcastExceptOneAddress(Packet packet, String address);
	
	public void connectToDevice(String address, int numberOfRetries);
	
	public void disconnectFromDevice(String address);
	
	public void cancelAll();
}
