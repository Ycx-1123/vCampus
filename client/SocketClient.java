package vCampus.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.awt.Window;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import vCampus.common.Message;
import vCampus.common.vo.Student;
import vCampus.common.vo.Teacher;
import vCampus.common.vo.Admin;

/**
 * 客户端 Socket 通信工具类，提供统一的同步收发接口
 */
public class SocketClient {
    private static final String SERVER_IP = "127.0.0.1"; // 本地测试 IP
    private static final int SERVER_PORT = 8888;        // 服务端端口

    // ★ 全局保存当前登录的用户对象，方便提取一卡通号 (Globally save the current logged-in user object)
    public static Object currentUser = null; 

    /**
     * 从用户对象中提取一卡通号 (Extract card ID from user object)
     */
    public static String extractCard(Object user) {
        if (user instanceof Student) return ((Student) user).getSCard();
        if (user instanceof Teacher) return ((Teacher) user).getTeacherCard();
        if (user instanceof Admin) return ((Admin) user).getACard();
        return null;
    }

    /**
     * 发送 Message 请求并获取服务端返回的 Message 响应
     */
    public static Message send(Message request) {
        // ★ 发送前，如果请求还没绑定发送者ID，且当前有用户登录，则自动附加上一卡通号 
        // (Attach senderId automatically before sending if a user is logged in)
        if (currentUser != null && request.getSenderId() == null) {
             request.setSenderId(extractCard(currentUser));
        }

        try (
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            // 发送请求给服务端
            out.writeObject(request);
            out.flush();

            // 接收并返回服务端的响应
            Message response = (Message) in.readObject();

            // ★ 全局拦截强制下线信号 (Global interceptor for forced logout)
            if (!response.isSuccess() && "FORCE_LOGOUT".equals(response.getResponseMsg())) {
                SwingUtilities.invokeLater(() -> {
                    // 弹出提示框 (Show alert dialog)
                    JOptionPane.showMessageDialog(null, 
                        "您的账号已在其他终端登录，或密码已被修改，您已被强制下线！", 
                        "安全提示", 
                        JOptionPane.ERROR_MESSAGE);
                        
                    // 关闭所有窗口 (Close all opened windows)
                    for (Window window : Window.getWindows()) {
                        window.dispose();
                    }
                    
                    // 清空当前用户状态并重回登录页 (Clear current user state and return to LoginFrame)
                    currentUser = null;
                    try {
                        // 注意：这里请确保你的 LoginFrame 类路径正确，如果不同请自行修改 import
                        // (Ensure the path to LoginFrame is correct)
                        Class<?> loginFrameClass = Class.forName("vCampus.client.view.LoginFrame");
                        Window loginFrame = (Window) loginFrameClass.getDeclaredConstructor().newInstance();
                        loginFrame.setVisible(true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                
                // 将当前状态重置，防止前端继续处理 (Reset current state to prevent frontend processing)
                response.setResponseMsg("强制下线"); 
            }

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            Message errorResponse = new Message();
            errorResponse.setSuccess(false);
            errorResponse.setResponseMsg("网络连接异常，请检查服务端是否启动。");
            return errorResponse;
        }
    }
}