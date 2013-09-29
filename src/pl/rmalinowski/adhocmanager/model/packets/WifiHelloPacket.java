package pl.rmalinowski.adhocmanager.model.packets;

public class WifiHelloPacket extends Packet {

	private static final long serialVersionUID = 1L;

	public WifiHelloPacket(String macAddress) {
		super();
		this.macAddress = macAddress;
	}

	private String macAddress;

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	
	
}
