package pl.rmalinowski.adhocmanager.services;

import pl.rmalinowski.adhocmanager.api.NetworkLayerService;
import pl.rmalinowski.adhocmanager.api.PhysicalLayerService;
import pl.rmalinowski.adhocmanager.api.impl.AodvService;
import pl.rmalinowski.adhocmanager.api.impl.BluetoothService;

public class ContainerService {

	public static final String NETWORK_SERVICE = "networkService";
	public static final String PHYSICAL_SERVICE = "physicalService";
	private PhysicalLayerService physicalService;
	private NetworkLayerService networkService;
	private static ContainerService instance = null;

	public static ContainerService getInstance() {
		if (instance == null) {
			synchronized (ContainerService.class) {
				if (instance == null) {
					instance = new ContainerService();
				}
			}
		}
		return instance;
	}

	public ContainerService() {
		initialize();
	}

	public void initialize() {


	}

}
