package pl.rmalinowski.adhocmanager.model.packets;

import java.io.Serializable;

public class DataPacket extends Packet {

	private static final long serialVersionUID = 6637195236740189077L;
	private Serializable data;
	//adres docelowy do ktorego skierowana jest wiadomosc
	private String destinationAddress;
	//adres nadawcy wiadomosci
	private String sourceAddress;
	
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

	public String getDestinationAddress() {
		return destinationAddress;
	}

	public void setDestinationAddress(String destinationAddress) {
		this.destinationAddress = destinationAddress;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public void setSourceAddress(String sourceAddress) {
		this.sourceAddress = sourceAddress;
	}

}
