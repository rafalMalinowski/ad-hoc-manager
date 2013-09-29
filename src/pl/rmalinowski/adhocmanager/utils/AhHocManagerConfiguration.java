package pl.rmalinowski.adhocmanager.utils;

import pl.rmalinowski.adhocmanager.api.impl.AodvService;
import pl.rmalinowski.adhocmanager.api.impl.BluetoothService;

public class AhHocManagerConfiguration {
	public final static Integer ROUTING_TABLE_REFRESH_INTERVAL = 1000;

	@SuppressWarnings("rawtypes")
//	public final static Class physicalLayerClass = WiFiDirectService.class;
	public final static Class physicalLayerClass = BluetoothService.class;
	@SuppressWarnings("rawtypes")
	public final static Class networkLayerClass = AodvService.class;
	
}
