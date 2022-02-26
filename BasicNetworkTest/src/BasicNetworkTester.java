import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BasicNetworkTester {

	
	private static Boolean isClient = false;

	private static String ServerAddress = "127.0.0.1";
	private static int ServerPort = 1234;
	
	private static Socket clientSocket;
	private static ServerSocket serverSocket;
	private static PrintWriter out;
	private static BufferedReader in;
	private static BufferedReader stdIn;
	private static String userInput = null;
	
	
	public static void main(String[] args) {
		
		//Create User InputReader
		stdIn = new BufferedReader(new InputStreamReader(System.in));
		
		//Decide if Client or Server
		System.out.println("Beginning Network Test.");
		System.out.println("--------------------------------");
		System.out.println("Am I a Client('1') or a Server('0')?");
		System.out.println("Please enter 1 or 0...");
		try {
			while ((userInput = stdIn.readLine()) != null) {
				if(Integer.valueOf(userInput) == 1) {
					System.out.println("I am now a Client!");
					isClient = true;
				}else {
					System.out.println("I am now a Server!");
					isClient = false;
				}
			    break;
			}
		} catch (IOException e1) {
			System.err.println("Error while deciding if I am a Client or Server!");
			e1.printStackTrace();
		}
		
		
		//Query Communication Data
		queryComData();
		
		//SetUp Sockets
		if (isClient){
			
			clientRole();
			while (true) {
				try {
					System.out.println("--------------------------------");
					System.out.println("Please enter a Message for the Server...");
					while ((userInput = stdIn.readLine()) != null) {
						System.out.println("Sending Message.");
					    out.println(userInput);
					    System.out.println("Waiting for Response...");
					    System.out.println("Received Response: " + in.readLine());
					    break;
					}
				} catch (IOException e) {
					System.err.println("Error during Client Communication Loop!");
					e.printStackTrace();
				}
			}
			
		}else {
			
			serverRole();
			while(true) {
				String inputLine = null;
				String outputLine = null;
				try {
					System.out.println("--------------------------------");
					System.out.println("Please wait for a Message from the Client...");
					while ((inputLine = in.readLine()) != null) {
						System.out.println("Server received Message:");
						System.out.println(inputLine);
						System.out.println("Please enter Response...");
						while ((userInput = stdIn.readLine()) != null) {
							System.out.println("Sending: " + userInput);
							outputLine = userInput;
							break;
						}
					    out.println(outputLine);
					    break;
					}
				} catch (IOException e) {
					System.err.println("Error during Server Communication Loop!");
					e.printStackTrace();
				}
			}
		}
		

	}
	
	
	public static void clientRole() {
		System.out.println("I am acting as the Client.");
		
		//Create Client Socket
		try {
			System.out.println("Creating new ClientSocket.");
			clientSocket = new Socket(ServerAddress, ServerPort);
		} catch (IOException e) {
			System.err.println("Error during creation of ClientSocket!");
			e.printStackTrace();
		}
		
		//Create Client OutputWriter
		try {
			System.out.println("Creating new ClientOutput.");
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		} catch (IOException e) {
			System.err.println("Error during creation of ClientOutput!");
			e.printStackTrace();
		}
		
		//Create Client InputReader
		try {
			System.out.println("Creating new ClientInput.");
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {
			System.err.println("Error during creation of ClientInput!");
			e.printStackTrace();
		}
		
	}
	
	
	public static void serverRole() {
		System.out.println("I am acting as the Server.");
		
		//Create Server Socket
		try {
			System.out.println("Creating new ServerSocket.");
			serverSocket = new ServerSocket(ServerPort);
		} catch (IOException e) {
			System.err.println("Error during creation of ServerSocket!");
			e.printStackTrace();
		}
		
		//Accept Client Connection
		try {
			System.out.println("Accepting Client Connection.");
			clientSocket = serverSocket.accept();
		} catch (IOException e) {
			System.err.println("Error during accepting of client connection!");
			e.printStackTrace();
		}
		
		
		//Create Server OutputWriter
		try {
			System.out.println("Creating new ServerOutput.");
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		} catch (IOException e) {
			System.err.println("Error during creation of ServerOutput!");
			e.printStackTrace();
		}
		
		//Create Client InputReader
		try {
			System.out.println("Creating new ServerInput.");
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {
			System.err.println("Error during creation of ServerInput!");
			e.printStackTrace();
		}
		
	}
	
	
	public static void queryComData(){
		System.out.println("--------------------------------");
		System.out.println("Beginning SetUp...");
		try {
			System.out.println("Please enter the Server IP...");
			while ((userInput = stdIn.readLine()) != null) {
				System.out.println("Setting Server IP to: " + userInput);
				ServerAddress = userInput;
				System.out.println("Please enter the Server Port...");
				while ((userInput = stdIn.readLine()) != null) {
					System.out.println("Setting Server Port to: " + userInput);
					ServerPort = Integer.valueOf(userInput);
				    break;
				}
			    break;
			}
		System.out.println("SetUp completed.");
		} catch (IOException e) {
			System.err.println("Error during SetUp!");
			e.printStackTrace();
		}
	}
}
