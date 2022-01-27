package main.networkConnection;

import java.io.Serializable;

/**A wrapper for a String Transmission that includes a head String that is used to identify the transmission type/purpose.
 * 
 * @author Jonas Huehne
 *
 */
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
