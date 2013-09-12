package pl.rmalinowski.adhocmanager.model.packets;

public class DataPacket implements Packet {

	private static final long serialVersionUID = 6637195236740189077L;
	private Object data;

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}
