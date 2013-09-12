package pl.rmalinowski.adhocmanager.model;

import java.io.Serializable;

public class PhysicalLayerEvent implements Serializable {
	
	private static final long serialVersionUID = -3755487624475015937L;

	public PhysicalLayerEvent(PhysicalLayerEventType eventType){
		super();
		this.eventType = eventType;
	}
	
	public PhysicalLayerEvent(PhysicalLayerEventType eventType, Object data) {
		super();
		this.eventType = eventType;
		Data = data;
	}

	private PhysicalLayerEventType eventType;
	private Object Data;
	
	public PhysicalLayerEventType getEventType() {
		return eventType;
	}
	public void setEventType(PhysicalLayerEventType eventType) {
		this.eventType = eventType;
	}
	public Object getData() {
		return Data;
	}
	public void setData(Object data) {
		Data = data;
	}
	

}
