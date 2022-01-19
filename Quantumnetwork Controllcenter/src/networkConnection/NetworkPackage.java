package networkConnection;

import java.io.Serializable;

public class NetworkPackage implements Serializable{

	public String head;
	public String content;
	
	public NetworkPackage(String head, String content) {
		this.head = head;
		this.content = content;
	}
	
	public String getHead() {
		return head;
	}
	
	public String getContent() {
		return content;
	}
}
