package pl.rmalinowski.adhocmanager.model;

public class Node {
	
	private String address;
	private String name;
	private long id;
	public Node(){
		
	}
	public Node(String address, String name) {
		super();
		this.address = address;
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	
}
