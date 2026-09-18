package vCampus.client;

import vCampus.common.Message;

public class Main {
    public static void main(String[] args) {
        // 1. 构造发给服务端的测试消息（请求类型为 "TEST"）
        Message requestMsg = new Message("TEST", "Hello Server!");

        // 2. 调用 SocketClient 发送消息，并获取服务端返回的响应
        System.out.println("正在发送测试请求到服务端...");
        Message responseMsg = SocketClient.send(requestMsg);

        // 3. 打印接收到的服务端响应结果
        System.out.println("\n===== 收到服务端响应结果 =====");
        System.out.println("处理状态 (isSuccess) : " + responseMsg.isSuccess());
        System.out.println("返回信息 (responseMsg): " + responseMsg.getResponseMsg());
    }    
}