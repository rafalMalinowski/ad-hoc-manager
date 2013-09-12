package pl.rmalinowski.adhocmanager.model.packets;

import java.io.Serializable;

public class DataPacket implements Packet {

	private static final long serialVersionUID = 6637195236740189077L;
	private Serializable data;
	

	public DataPacket(Serializable data) {
		super();
		this.data = data;
	}

	public Serializable getData() {
		return data;
	}

	public void setData(Serializable data) {
		this.data = data;
	}

}
