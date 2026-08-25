package vCampus.common;

import java.io.Serializable;

/**
 * Socket 客户端与服务端通信的统一消息载体类
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;     // 消息类型/请求操作（如："LOGIN", "QUERY_BOOKS", "ADD_COURSE" 等）
    private Object data;     // 客户端发给服务端的数据，或服务端返回的数据对象
    private boolean success; // 服务端处理结果状态（true: 成功, false: 失败）
    private String responseMsg; // 服务端返回的文字提示（如："登录成功" 或 "密码错误"）

    public Message() {
    }

    public Message(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    // Getter 与 Setter 方法
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(String responseMsg) {
        this.responseMsg = responseMsg;
    }
}
