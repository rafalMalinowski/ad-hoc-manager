package pl.rmalinowski.adhocmanager.api;

import android.app.Service;
import android.os.Binder;

public abstract class NetworkLayerService extends Service implements
		INetworkLayerService {
	
	public static final String NETWORK_LAYER_MESSAGE = "networkLayerMessage";
	public static final String NETWORK_LAYER_MESSAGE_TYPE = "networkLayerMessageType";
	public static final String PHYSICAL_LAYER_CLASS = "physicalClass";
	
	public class MyBinder extends Binder {
		public INetworkLayerService getService() {
			return NetworkLayerService.this;
		}
	}
}
