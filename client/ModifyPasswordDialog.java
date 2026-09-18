package vCampus.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Properties;
import java.util.Random;

// 引入 JavaMail 相关的包 (确保项目中已包含 javax.mail 依赖)
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import vCampus.common.Message;
import vCampus.common.vo.Student;
import vCampus.common.vo.Teacher;
import vCampus.common.vo.Admin;

/**
 * 密码修改与重置弹窗 (精简提示风格)
 */
public class ModifyPasswordDialog extends JDialog {

    private JTextField txtCard;
    private JTextField txtPhone;
    private JTextField txtVerifyCode;
    private JButton btnGetCode;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;

    // 存储当前生成的验证码，用于后续比对
    private String generatedCode = "";
    private long codeGenerateTime = 0; // 新增：记录验证码生成的时间戳

    public ModifyPasswordDialog(Frame owner) {
        super(owner, "重置密码", true);
        setUndecorated(true);
        setSize(400, 540); 
        setLocationRelativeTo(owner);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(LoginFrame.CARD_BG);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LoginFrame.FRAME_BORDER, 2, true),
            new EmptyBorder(25, 30, 20, 30)
        ));

        // 1. 顶部标题
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(LoginFrame.CARD_BG);
        
        JLabel lblTitle = new JLabel("修改 / 找回密码");
        lblTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        lblTitle.setForeground(new Color(50, 50, 50));
        headerPanel.add(lblTitle, BorderLayout.WEST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. 表单输入区
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(LoginFrame.CARD_BG);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtCard = createInputField("一卡通账号");
        gbc.gridy = 0;
        formPanel.add(createLabeledComponent("一卡通账号：", txtCard), gbc);

        txtPhone = createInputField("预留手机号");
        gbc.gridy = 1;
        formPanel.add(createLabeledComponent("手机号：", txtPhone), gbc);

        // 验证码输入与获取按钮区域
        JPanel codePanel = new JPanel(new BorderLayout(8, 0));
        codePanel.setBackground(LoginFrame.CARD_BG);
        txtVerifyCode = createInputField("6位验证码");
        txtVerifyCode.setPreferredSize(new Dimension(200, 36));
        
        btnGetCode = new JButton("获取验证码");
        btnGetCode.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        btnGetCode.setBackground(new Color(240, 242, 245));
        btnGetCode.setForeground(new Color(60, 60, 60));
        btnGetCode.setFocusPainted(false);
        btnGetCode.setBorder(BorderFactory.createLineBorder(LoginFrame.FRAME_BORDER, 1, true));
        btnGetCode.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGetCode.addActionListener(e -> processGetCode());

        codePanel.add(txtVerifyCode, BorderLayout.CENTER);
        codePanel.add(btnGetCode, BorderLayout.EAST);
        
        gbc.gridy = 2;
        formPanel.add(createLabeledComponent("验证码：", codePanel), gbc);

        txtNewPassword = createPasswordField("请输入新密码");
        gbc.gridy = 3;
        formPanel.add(createLabeledComponent("新密码：", txtNewPassword), gbc);

        txtConfirmPassword = createPasswordField("请再次输入新密码");
        gbc.gridy = 4;
        formPanel.add(createLabeledComponent("确认新密码：", txtConfirmPassword), gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // 3. 底部按钮区
        JPanel footerPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        footerPanel.setBackground(LoginFrame.CARD_BG);
        footerPanel.setBorder(new EmptyBorder(15, 0, 5, 0));

        JButton btnCancel = new JButton("取 消");
        btnCancel.setPreferredSize(new Dimension(100, 38));
        btnCancel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        btnCancel.setBackground(new Color(240, 242, 245));
        btnCancel.setForeground(new Color(100, 100, 100));
        btnCancel.setFocusPainted(false);
        btnCancel.setBorder(BorderFactory.createEmptyBorder());
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSubmit = new JButton("确认修改");
        btnSubmit.setPreferredSize(new Dimension(100, 38));
        btnSubmit.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        btnSubmit.setBackground(LoginFrame.BTN_GREEN);
        btnSubmit.setForeground(new Color(40, 70, 40));
        btnSubmit.setFocusPainted(false);
        btnSubmit.setBorder(BorderFactory.createEmptyBorder());
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSubmit.setBackground(new Color(160, 205, 165));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSubmit.setBackground(LoginFrame.BTN_GREEN);
            }
        });
        btnSubmit.addActionListener(e -> doModifyPassword());

        footerPanel.add(btnCancel);
        footerPanel.add(btnSubmit);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createLabeledComponent(String labelText, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(LoginFrame.CARD_BG);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        label.setForeground(new Color(90, 90, 90));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JTextField createInputField(String placeholder) {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(320, 36));
        field.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        field.setForeground(Color.DARK_GRAY);
        field.setBackground(new Color(250, 252, 251));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LoginFrame.FRAME_BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        return field;
    }

    private JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setPreferredSize(new Dimension(320, 36));
        field.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        field.setForeground(Color.DARK_GRAY);
        field.setBackground(new Color(250, 252, 251));
        field.setEchoChar('●');
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LoginFrame.FRAME_BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        return field;
    }

    /**
     * 处理获取验证码逻辑
     */
    private void processGetCode() {
        String card = txtCard.getText().trim();
        String phone = txtPhone.getText().trim();

        if (card.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入一卡通账号和预留手机号", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnGetCode.setEnabled(false);
        btnGetCode.setText("发送中...");

        // 在新线程中执行网络和邮件任务，避免 UI 卡顿
        new Thread(() -> {
            String resultStatus = verifyUserAndGetEmail(card, phone);

            if ("NOT_MATCH".equals(resultStatus)) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "账号或手机号不匹配", "错误", JOptionPane.ERROR_MESSAGE);
                    btnGetCode.setText("获取验证码");
                    btnGetCode.setEnabled(true);
                });
                return;
            }

            if ("NO_EMAIL".equals(resultStatus)) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "该账号未绑定电子邮箱，请联系管理员", "提示", JOptionPane.WARNING_MESSAGE);
                    btnGetCode.setText("获取验证码");
                    btnGetCode.setEnabled(true);
                });
                return;
            }

            if ("ERROR".equals(resultStatus) || resultStatus == null) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "连接服务器失败，请检查网络或服务端状态", "错误", JOptionPane.ERROR_MESSAGE);
                    btnGetCode.setText("获取验证码");
                    btnGetCode.setEnabled(true);
                });
                return;
            }

            String targetEmail = resultStatus;
            generatedCode = String.format("%06d", new Random().nextInt(999999));
            codeGenerateTime = System.currentTimeMillis(); // 新增：记录当前毫秒数
            
            boolean isSent = sendEmailViaQQ(targetEmail, generatedCode);

            SwingUtilities.invokeLater(() -> {
                if (isSent) {
                    JOptionPane.showMessageDialog(this, "验证码已发送至绑定邮箱", "提示", JOptionPane.INFORMATION_MESSAGE);
                    btnGetCode.setText("重新发送");
                } else {
                    JOptionPane.showMessageDialog(this, "验证码发送失败，请稍后重试", "错误", JOptionPane.ERROR_MESSAGE);
                    btnGetCode.setText("获取验证码");
                }
                btnGetCode.setEnabled(true);
            });

        }).start();
    }

    /**
     * 与服务端通信，验证一卡通和手机号是否匹配，并返回特定状态标识或有效邮箱
     */
    private String verifyUserAndGetEmail(String card, String phone) {
        try {
            Student reqStudent = new Student();
            reqStudent.setSCard(card);
            reqStudent.setSphone(phone);

            Message requestMsg = new Message();
            requestMsg.setType("VERIFY_USER_INFO"); 
            requestMsg.setData(reqStudent);

            try (Socket socket = new Socket("127.0.0.1", 8888);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject(requestMsg);
                out.flush();

                Message responseMsg = (Message) in.readObject();
                
                if (responseMsg != null) {
                    if (responseMsg.isSuccess()) {
                        Object responseData = responseMsg.getData();
                        
                        if (responseData instanceof Student) {
                            return ((Student) responseData).getSemail();
                        } else if (responseData instanceof Teacher) {
                            return ((Teacher) responseData).getEmail();
                        } else if (responseData instanceof Admin) {
                            return ((Admin) responseData).getAEmail();
                        } else if (responseData instanceof String) {
                            String targetEmail = (String) responseData;
                            if (targetEmail.trim().isEmpty()) {
                                return "NO_EMAIL";
                            }
                            return targetEmail.trim();
                        }
                    } else {
                        if (responseMsg.getResponseMsg() != null && responseMsg.getResponseMsg().contains("未查找到")) {
                            return "NO_EMAIL";
                        }
                        return "NOT_MATCH";
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "ERROR";
        }
        return "ERROR";
    }

    /**
     * 调用 QQ 邮箱 SMTP 发送邮件
     */
    private boolean sendEmailViaQQ(String toEmail, String code) {
        String senderEmail = "wushiwen5@qq.com";
        String authCode = "lnxmedydszwtecdj"; 

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.qq.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, authCode);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "vCampus 管理中心", "UTF-8"));
            message.addRecipient(RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("vCampus 平台密码修改验证码");
            
            String emailContent = "<h3>您好！</h3>"
                    + "<p>您正在尝试修改 vCampus 系统的登录密码。</p>"
                    + "<p>您的 6 位验证码是：<strong style='font-size:24px; color:#B7DCBB;'>" + code + "</strong></p>"
                    + "<p>请在十分钟内将此验证码填入系统中。如果不是您本人操作，请忽略此邮件。</p>";
            
            message.setContent(emailContent, "text/html;charset=UTF-8");
            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 提交修改密码请求 (精简提示)
     */
    private void doModifyPassword() {
        String card = txtCard.getText().trim();
        String phone = txtPhone.getText().trim();
        String inputCode = txtVerifyCode.getText().trim();
        String newPwd = new String(txtNewPassword.getPassword()).trim();
        String confirmPwd = new String(txtConfirmPassword.getPassword()).trim();

        // 1. 本地非空校验
        if (card.isEmpty() || phone.isEmpty() || inputCode.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请将表单填写完整", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

     // 2. 校验验证码及有效时间
        if (generatedCode.isEmpty() || !inputCode.equals(generatedCode)) {
            JOptionPane.showMessageDialog(this, "验证码错误", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - codeGenerateTime > 600000) { // 判断是否超过10分钟
            JOptionPane.showMessageDialog(this, "验证码已失效（超过10分钟），请重新获取", "错误", JOptionPane.ERROR_MESSAGE);
            generatedCode = ""; // 清空失效的验证码
            return;
        }

        // 3. 新旧密码二次确认校验
        if (!newPwd.equals(confirmPwd)) {
            JOptionPane.showMessageDialog(this, "两次输入的新密码不一致", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. 构建发送给服务端的最终修改请求
        try {
            Student reqStudent = new Student();
            reqStudent.setSCard(card);       
            reqStudent.setSphone(phone);     
            reqStudent.setSpassword(newPwd); 

            Message requestMsg = new Message();
            requestMsg.setType("RESET_PASSWORD");  
            requestMsg.setData(reqStudent); 

            try (Socket socket = new Socket("127.0.0.1", 8888);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject(requestMsg);
                out.flush();

                Message responseMsg = (Message) in.readObject();

                if (responseMsg != null && responseMsg.isSuccess()) {
                    JOptionPane.showMessageDialog(this, "密码重置成功", "提示", JOptionPane.INFORMATION_MESSAGE);
                    dispose(); // 点击确定后关闭
                } else {
                    JOptionPane.showMessageDialog(this, "密码重置失败", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "网络连接异常，请检查服务端状态", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}