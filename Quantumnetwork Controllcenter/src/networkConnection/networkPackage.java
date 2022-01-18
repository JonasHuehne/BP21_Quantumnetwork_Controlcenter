package networkConnection;

import java.io.Serializable;

public class networkPackage implements Serializable{

	public String head;
	public String content;
	
	public networkPackage(String head, String content) {
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
