package pl.rmalinowski.adhocmanager.api;

import java.io.Serializable;
import java.util.List;

public interface INetworkLayerService{
	
	public void sendData(Serializable data, String address);
	
	public void sendBroadcastData(Serializable data);
	
	public void reInitialize();
	
	public void searchForDevices();
	
	public void connectToNeighbours();
	
	public List<String> getNodes();
	
	public void test1();

	public void test2();

	public void test3();
	
}
