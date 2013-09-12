package pl.rmalinowski.adhocmanager.api;

import android.app.Service;
import android.os.Binder;

public abstract class PhysicalLayerService extends Service implements
		IPhysicalLayerService {

	public static final String PHYSICAL_LAYER_MESSAGE = "physicalLayerMessage";
	public static final String PHYSICAL_LAYER_MESSAGE_TYPE = "physicalLayerMessageType";
	
	public class MyBinder extends Binder {
		public IPhysicalLayerService getService() {
			return PhysicalLayerService.this;
		}
	}
}
