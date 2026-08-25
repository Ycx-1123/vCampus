package vCampus.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import vCampus.common.Message;

/**
 * Socket 服务端主程序，负责监听客户端连接并开启多线程处理请求
 */
public class Server {
    private static final int PORT = 8888; // 服务端监听端口号

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("服务端启动成功，正在监听端口 " + PORT + "...");
            
            while (true) {
                // 阻塞等待客户端连接
                Socket socket = serverSocket.accept();
                System.out.println("检测到新客户端连接：" + socket.getInetAddress());
                
                // 为每个客户端连接开启独立线程处理
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 客户端请求处理线程类
     */
    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
            ) {
                // 读取客户端发来的请求 Message 对象
                Message request = (Message) in.readObject();
                System.out.println("收到请求类型：" + request.getType());

                // 构造响应 Message 对象
                Message response = new Message();
                response.setSuccess(true);
                response.setResponseMsg("服务端收到请求，通信成功！");

                // 将响应发回客户端
                out.writeObject(response);
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}