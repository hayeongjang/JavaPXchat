package application;
	
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;



public class Main extends Application {
	
	// 다양한 클라이언트가 접속 했을 때 스레드를 효과적으로 관리 
	
	// threadPool 이용, 다양한 클라이언트 접속 했을 때 스레드들 효과적으로 관리, 한정된 자원을 이용해 안정적으로 서버운영
	public static ExecutorService threadPool;   
	
	//접속한 클라이언트 관리
	public static Vector<Client> clients = new Vector<Client>(); 
	
	ServerSocket serverSocket;
	
	// 서버를 구동시켜서 클라이언트의 연결을 기다리는 메소드
	public void startServer(String IP , int port) {
		try {
			serverSocket = new ServerSocket();
			
			// 소켓 통신은 소켓에 대한 객체를 활성, 서버 역할 수행하는 컴퓨터가 특정한 클라이언트 접속 기다림
			serverSocket.bind(new InetSocketAddress(IP, port));  
		} catch (Exception e) {
			e.printStackTrace();
			if(!serverSocket.isClosed()) {
				stopServer(); 
			}
			return;
		}
		// 클라이언트가 접속할 때까지 계속 기다리는 스레드
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						// 새로운 클라이언트가 접속할 수 있게 만들어줌
						Socket socket = serverSocket.accept();
						
						// 새롭게 접속한 클라이언트 추가
						clients.add(new Client(socket));   
						System.out.println("[클라이언트 접속]"
							+ socket.getRemoteSocketAddress()
							+ ": " + Thread.currentThread().getName());
					} catch (Exception e) {
						if(!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}
			}
		};
		// threadPool 초기화
		threadPool = Executors.newCachedThreadPool(); 
		
		// threadPool에 현재 클라이언트를 기다리는 스레드를 담을 수 있게 처리
		threadPool.submit(thread);   
	}
	
	// 서버의 작동을 중지시키는 메소드
	public void stopServer() {
		try {
			// 현재 작동중인 모든 소켓 닫기
			
			// 모든 클라이언트에 개별적으로 접근
			Iterator<Client> iterator = clients.iterator(); 
			
			// 하나씩 접속
			while(iterator.hasNext()) {   
				Client client = iterator.next();
				client.socket.close();
				iterator.remove();
			}
			// 서버 소켓 객체 닫기
			if(serverSocket != null && ! serverSocket.isClosed()) { 
				serverSocket.close();
			}
			// 스레드 풀 종료하기
			// 자원 할당 해제
			if(threadPool != null && ! threadPool.isShutdown()) { 
				threadPool.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// UI를 생성하고, 실질적으로 프로그램을 동작시키는 (사용자에게 보여지는) 메소드
	@Override
	public void start(Stage primaryStage) {
		// 레이아웃 생성
		BorderPane root = new BorderPane(); 
		root.setPadding(new Insets(5));
		
		// 긴 문장의 텍스트가 담기는 공간
		TextArea textArea = new TextArea();  
		
		// 수정불가, 어떠한 문장을 단순히 출력만 가능하게 함
		textArea.setEditable(false);  
		
		textArea.setFont(new Font("나눔 고딕", 15));
		root.setCenter(textArea); 
		
		Button toggleButton = new Button("시작하기");
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		BorderPane.setMargin(toggleButton, new Insets(1, 0, 0, 0));
		root.setBottom(toggleButton);
		
		String IP = "127.0.0.1";  
		int port = 1234;
		
		// 사용자가 토글버튼 눌렀을 때 이번트 발생
		toggleButton.setOnAction(event -> {  
		
			if(toggleButton.getText().equals("시작하기")) {
				startServer(IP, port);
				Platform.runLater(()-> { 
					String message = String.format("[서버 시작]\n",IP, port);
					textArea.appendText(message);
					toggleButton.setText("종료하기");
					});
				} else {
					stopServer();
					Platform.runLater(()-> {
						String message = String.format("[서버 종료]\n",IP, port);
						textArea.appendText(message);
						toggleButton.setText("시작하기");
						});
					
				}
		});
		Scene scene = new Scene(root, 400, 400);
		primaryStage.setTitle("[채팅 서버]");
		primaryStage.setOnCloseRequest(event -> stopServer());
		primaryStage.setScene(scene);
		primaryStage.show();
	}
		
	// 프로그램의 진입점
	public static void main(String[] args) {
		launch(args);
	}
}
