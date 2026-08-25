package vCampus.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import vCampus.common.Message;

/**
 * 客户端 Socket 通信工具类，提供统一的同步收发接口
 */
public class SocketClient {
    private static final String SERVER_IP = "127.0.0.1"; // 本地测试 IP
    private static final int SERVER_PORT = 8888;        // 服务端端口

    /**
     * 发送 Message 请求并获取服务端返回的 Message 响应
     */
    public static Message send(Message request) {
        try (
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            // 发送请求给服务端
            out.writeObject(request);
            out.flush();

            // 接收并返回服务端的响应
            return (Message) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            Message errorResponse = new Message();
            errorResponse.setSuccess(false);
            errorResponse.setResponseMsg("网络连接异常，请检查服务端是否启动。");
            return errorResponse;
        }
    }
}