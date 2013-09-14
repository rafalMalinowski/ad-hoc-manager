package pl.rmalinowski.adhocmanager.events;

import java.io.Serializable;

public class NetworkLayerEvent implements Serializable {

	private static final long serialVersionUID = -2717230231352749272L;

	public NetworkLayerEvent(NetworkLayerEventType eventType) {
		super();
		this.eventType = eventType;
	}

	public NetworkLayerEvent(NetworkLayerEventType eventType, Object data) {
		super();
		this.eventType = eventType;
		this.data = data;
	}

	private NetworkLayerEventType eventType;
	private Object data;

	public NetworkLayerEventType getEventType() {
		return eventType;
	}

	public void setEventType(NetworkLayerEventType eventType) {
		this.eventType = eventType;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}
