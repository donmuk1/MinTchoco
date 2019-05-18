package com.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChattingServer {
	ExecutorService executorService;
	MyServerFrame sf;
	
	ServerSocket serverSocket;
	List<Client> connections=new Vector<Client>();
	
	public ChattingServer(MyServerFrame sf){
		this.sf=sf;
	}
	
	void startServer(){
		executorService=Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors());
		try{
			serverSocket=new ServerSocket(5001);
		}catch(Exception e){
			if(!serverSocket.isClosed()){
				stopServer();
			}
			return;
		}
		Runnable runnable=new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				sf.displayText("[서버 시작]");
				sf.btnStartStop.setText("stop");
				while(true){
					try{
						Socket socket=serverSocket.accept();
						String message="[연결 수락: "+ socket.getRemoteSocketAddress()
							+":"+Thread.currentThread()+"]";
						sf.displayText(message);
						Client client=new Client(socket);
						connections.add(client);
						sf.displayText("[연결개수 :"+connections.size()+"]");
					}catch(Exception e){
						if(!serverSocket.isClosed()){
							stopServer();
						}
						break;
					}
				}
			}
			
		};
		executorService.submit(runnable);
	}
	
	void stopServer(){
		try{
			Iterator<Client> iterator =connections.iterator();
			while(iterator.hasNext()){
				Client client=iterator.next();
				client.socket.close();
				iterator.remove();
			}
			if(serverSocket!=null&&!serverSocket.isClosed()){
				serverSocket.close();
			}
			if(executorService!=null && !executorService.isShutdown()){
				executorService.shutdown();
			}
			sf.displayText("[서버멈춤]");
			sf.btnStartStop.setText("start");
		}catch(Exception e){}
		
	}
	
	public class Client {
		Socket socket;
		
		Client(Socket socket){
			this.socket=socket;
			receive();
		}
		
		void receive(){
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						while(true) {
							byte[] byteArr = new byte[100];
							InputStream inputStream = socket.getInputStream();
							
							//클라이언트가 비정상적으로 종료되면 IOException이 발생
							int readByteCount = inputStream.read(byteArr);
							
							//클라이언트가 정상적으로 Socket의 close()를 호출했을 경우
							if(readByteCount == -1) {  throw new IOException(); }
							
							String message = "[요청처리: " + socket.getRemoteSocketAddress() 
								+ ": " + Thread.currentThread().getName() + "]";
							sf.displayText(message);
							
							String data = new String(byteArr, 0, readByteCount, "UTF-8");
							
							for(Client client : connections) {
								client.send(data); 
							}
						}
					} catch(Exception e) {
						try {
							connections.remove(Client.this);
							String message = "[클라이언트 통신이 안됨: " + socket.getRemoteSocketAddress() 
								+ ": " + Thread.currentThread().getName() + "]";
							sf.displayText(message);
							socket.close();
						} catch (IOException e2) {}
					}
				}
			};
			executorService.submit(runnable);
			
		}
		void send(String data){
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						byte[] byteArr = data.getBytes("UTF-8");
						OutputStream outputStream = socket.getOutputStream();
						outputStream.write(byteArr);
						outputStream.flush();
					} catch(Exception e) {
						try {
							String message = "[클라이언트 통신이 안됨: " + socket.getRemoteSocketAddress() 
								+ ": " + Thread.currentThread().getName() + "]";
							sf.displayText(message);
							connections.remove(Client.this);
							socket.close();
						} catch (IOException e2) {}
					}
				}
			};
			executorService.submit(runnable);
		}
	}
}


