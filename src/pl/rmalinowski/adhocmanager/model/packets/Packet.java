package pl.rmalinowski.adhocmanager.model.packets;

import java.io.Serializable;

public abstract class Packet implements Serializable {

	private static final long serialVersionUID = 8007357319199891520L;
	
	private String interfaceAddress;
	private Integer ttl;
	
	public String getInterfaceAddress() {
		return interfaceAddress;
	}
	public void setInterfaceAddress(String interfaceAddress) {
		this.interfaceAddress = interfaceAddress;
	}
	public Integer getTtl() {
		return ttl;
	}
	public void setTtl(Integer ttl) {
		this.ttl = ttl;
	}
	
	

}
