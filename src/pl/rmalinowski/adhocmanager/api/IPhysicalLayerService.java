package pl.rmalinowski.adhocmanager.api;

import pl.rmalinowski.adhocmanager.model.packets.Packet;

public interface IPhysicalLayerService {

	public void initialize();
	
	public void sendPacket(Packet packet, String destination);
	
	public void searchForNeighbours();

	public void connectToNeighbours();
	
	public void getConnectedDevices();
}
