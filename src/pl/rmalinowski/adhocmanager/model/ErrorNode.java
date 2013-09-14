package pl.rmalinowski.adhocmanager.model;

import java.io.Serializable;

public class ErrorNode implements Serializable{

	private static final long serialVersionUID = 1907703865640648353L;
	private String address;
	private Integer sequenceNumber;
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public Integer getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	
}
