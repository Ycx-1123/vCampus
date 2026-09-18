package vCampus.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import vCampus.common.Message;
import vCampus.common.vo.Admin;
import vCampus.common.vo.Student;
import vCampus.common.vo.Teacher;

public class LoginFrame extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    // --- 色卡与视觉规范 ---
    public static final Color WINDOW_BG = new Color(242, 250, 253);   // 整体外围大背景（浅蓝）
    public static final Color CARD_BG = Color.WHITE;                    // 左右卡片内部背景（纯白）
    public static final Color FRAME_BORDER = new Color(216, 237, 230); // 优雅的淡蓝色边框线
    public static final Color BTN_GREEN = new Color(183, 220, 187);     // 主题绿按钮
    public static final Color TEXT_GRAY = new Color(150, 150, 150);

    public LoginFrame() {
        setTitle("vCampus - 身份认证中心");
        setSize(920, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        initUI();
    }

    private void initUI() {
        // 主面板：外围带有舒适的留白边距 (Padding)
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        mainPanel.setBackground(WINDOW_BG);
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // ==========================================
        // 1. 左侧卡片：插画展示区
        // ==========================================
        JPanel leftCard = new JPanel(new BorderLayout()) {
            private Image bgImage = loadIllustrationImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(234, 244, 235));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        leftCard.setBackground(CARD_BG);
        leftCard.setBorder(BorderFactory.createLineBorder(FRAME_BORDER, 2, true));
        mainPanel.add(leftCard);

        // ==========================================
        // 2. 右侧卡片：纯白登录表单区
        // ==========================================
        JPanel rightCard = new JPanel(new GridBagLayout());
        rightCard.setBackground(CARD_BG);
        rightCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FRAME_BORDER, 2, true),
            new EmptyBorder(10, 30, 10, 30)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 2.1 Logo
        ImageIcon originalIcon = loadLogoImage();
        JLabel lblLogo;
        if (originalIcon != null && originalIcon.getIconWidth() > 0) {
            int targetWidth = 220;
            int targetHeight = (originalIcon.getIconHeight() * targetWidth) / originalIcon.getIconWidth();
            Image scaledImg = originalIcon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            lblLogo = new JLabel(new ImageIcon(scaledImg));
        } else {
            lblLogo = new JLabel("vCampus");
            lblLogo.setFont(new Font("Microsoft YaHei", Font.BOLD, 28));
        }
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        rightCard.add(lblLogo, gbc);

        // 2.2 欢迎标语
        JLabel lblWelcome = new JLabel("欢迎登录虚拟校园平台");
        lblWelcome.setFont(new Font("Microsoft YaHei", Font.BOLD, 17));
        lblWelcome.setForeground(new Color(60, 60, 60));
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        rightCard.add(lblWelcome, gbc);

        // 2.3 账号输入框
        txtUser = createHintTextField("一卡通号/唯一ID");
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 12, 0);
        rightCard.add(txtUser, gbc);

        // 2.4 密码输入框
        txtPassword = createHintPasswordField("请输入密码");
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 20, 0);
        rightCard.add(txtPassword, gbc);

        // 2.5 登录按钮
        btnLogin = new JButton("登 录");
        btnLogin.setPreferredSize(new Dimension(260, 42));
        btnLogin.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        btnLogin.setForeground(new Color(40, 70, 40));
        btnLogin.setBackground(BTN_GREEN);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorder(BorderFactory.createEmptyBorder());
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnLogin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnLogin.setBackground(new Color(160, 205, 165));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnLogin.setBackground(BTN_GREEN);
            }
        });
        btnLogin.addActionListener(e -> doLogin());
        
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 12, 0);
        rightCard.add(btnLogin, gbc);

        // 2.6 底部辅助工具（遇到问题 & 修改密码）
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(CARD_BG);
        footerPanel.setPreferredSize(new Dimension(260, 22));

        // 左下角：遇到问题？
        JLabel lblHelp = new JLabel("遇到问题？");
        lblHelp.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        lblHelp.setForeground(new Color(150, 160, 150));
        lblHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblHelp.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                showHelpDialog();
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                lblHelp.setForeground(new Color(100, 150, 110));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                lblHelp.setForeground(new Color(150, 160, 150));
            }
        });
        footerPanel.add(lblHelp, BorderLayout.WEST);

        // 右下角：修改密码
        JLabel lblModifyPwd = new JLabel("修改密码");
        lblModifyPwd.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        lblModifyPwd.setForeground(new Color(100, 150, 110));
        lblModifyPwd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblModifyPwd.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                new ModifyPasswordDialog(LoginFrame.this).setVisible(true);
            }
        });
        footerPanel.add(lblModifyPwd, BorderLayout.EAST);

        gbc.gridy = 5;
        rightCard.add(footerPanel, gbc);

        mainPanel.add(rightCard);
        add(mainPanel);
        getRootPane().setDefaultButton(btnLogin);
    }

    /**
     * 左下角「遇到问题？」统一风格弹窗
     */
    /**
     * 左下角「遇到问题？」统一风格弹窗
     */
    private void showHelpDialog() {
        JDialog dialog = new JDialog(this, "技术支持", true);
        dialog.setUndecorated(true);
        // 将高度从 215 缩小至 185，消除多余的垂直间距
        dialog.setSize(360, 185); 
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FRAME_BORDER, 2),
            new EmptyBorder(20, 25, 20, 25)
        ));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(CARD_BG);

        // 标题
        JLabel lblTitle = new JLabel("遇到技术问题？");
        lblTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 17));
        lblTitle.setForeground(new Color(50, 50, 50));

        // 第一行说明：服务热线文本
        JLabel lblDetail = new JLabel("请致电技术支持热线：");
        lblDetail.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblDetail.setForeground(new Color(80, 80, 80));

        // 第二行：电话号码
        JLabel lblPhone = new JLabel("17508857006");
        lblPhone.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        lblPhone.setForeground(new Color(60, 110, 70)); 

        contentPanel.add(lblTitle);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(lblDetail);
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(lblPhone);

        // 修改按钮文字为“我已知晓”
        JButton btnClose = new JButton("我已知晓");
        btnClose.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        btnClose.setBackground(BTN_GREEN);
        btnClose.setForeground(new Color(40, 70, 40));
        btnClose.setFocusPainted(false);
        btnClose.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dialog.dispose());

        panel.add(contentPanel, BorderLayout.CENTER);
        panel.add(btnClose, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }
    private Image loadIllustrationImage() {
        String imgName = "969e3b9502d554d6b73631076cc78dcc.jpg";
        String[] paths = { imgName, "src/" + imgName, "src/vCampus/client/" + imgName };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) return new ImageIcon(path).getImage();
        }
        return null;
    }

    private ImageIcon loadLogoImage() {
        String[] paths = { "seu_logo.png", "src/seu_logo.png", "src/vCampus/client/seu_logo.png" };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) return new ImageIcon(path);
        }
        return null;
    }

    private JTextField createHintTextField(String hint) {
        JTextField field = new JTextField(hint);
        field.setPreferredSize(new Dimension(260, 40));
        field.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        field.setForeground(TEXT_GRAY);
        field.setBackground(new Color(250, 252, 251));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FRAME_BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(hint)) {
                    field.setText(""); field.setForeground(Color.DARK_GRAY);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(hint); field.setForeground(TEXT_GRAY);
                }
            }
        });
        return field;
    }

    private JPasswordField createHintPasswordField(String hint) {
        JPasswordField field = new JPasswordField(hint);
        field.setPreferredSize(new Dimension(260, 40));
        field.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        field.setForeground(TEXT_GRAY);
        field.setBackground(new Color(250, 252, 251));
        field.setEchoChar((char) 0); 
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FRAME_BORDER, 1, true),
            BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (new String(field.getPassword()).equals(hint)) {
                    field.setText(""); field.setEchoChar('●'); field.setForeground(Color.DARK_GRAY);
                }
            }
            public void focusLost(FocusEvent e) {
                if (new String(field.getPassword()).isEmpty()) {
                    field.setEchoChar((char) 0); field.setText(hint); field.setForeground(TEXT_GRAY);
                }
            }
        });
        return field;
    }

    private void doLogin() {
        String uid = txtUser.getText().trim();
        String pwd = new String(txtPassword.getPassword());

        if (uid.isEmpty() || uid.equals("一卡通号/唯一ID")) {
            JOptionPane.showMessageDialog(this, "请输入一卡通号！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Student loginUser = new Student();
            loginUser.setSCard(uid);     
            loginUser.setSpassword(pwd); 

            Message requestMsg = new Message();
            requestMsg.setData(loginUser);

            try (Socket socket = new Socket("127.0.0.1", 8888);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                
                out.writeObject(requestMsg);
                out.flush();
                
                Message responseMsg = (Message) in.readObject();
                
                // ★ 修改点 1：增加 isSuccess() 判断，确保业务逻辑走通
                if (responseMsg != null && responseMsg.isSuccess() && responseMsg.getData() != null) {
                    Object loggedInUser = responseMsg.getData();
                    
                    // ★ 修改点 2：将登录成功的用户赋值给 SocketClient，激活全局发送请求时的 senderId 拦截
                    SocketClient.currentUser = loggedInUser;
                    
                    String name = "";
                    String role = "";
                    Color roleColor = Color.WHITE;

                    if (loggedInUser instanceof Student) {
                        name = ((Student) loggedInUser).getSname();
                        role = "学生";
                        roleColor = new Color(229, 243, 243); 
                    } else if (loggedInUser instanceof Teacher) {
                        name = ((Teacher) loggedInUser).getTeacherName();
                        role = "教师";
                        roleColor = new Color(247, 220, 227); 
                    } else if (loggedInUser instanceof Admin) {
                        name = ((Admin) loggedInUser).getAName();
                        role = "管理员";
                        roleColor = new Color(254, 235, 220); 
                    }

                    showPremiumWelcomeDialog(name, role, roleColor, loggedInUser);

                } else {
                    // ★ 修改点 3：动态读取服务端返回的具体错误信息（如“已在其他终端登录”），如果为空才使用默认提示
                    String errorMsg = (responseMsg != null && responseMsg.getResponseMsg() != null) 
                                      ? responseMsg.getResponseMsg() : "一卡通号或密码错误！";
                    JOptionPane.showMessageDialog(this, errorMsg, "认证失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "网络连接失败，请检查 Server 是否启动", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPremiumWelcomeDialog(String name, String role, Color markerColor, Object loggedInUser) {
        JDialog dialog = new JDialog(this, true);
        dialog.setUndecorated(true);
        dialog.setSize(420, 240); 
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BTN_GREEN, 2),
            new EmptyBorder(25, 35, 25, 35) 
        ));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);
        
        JLabel lblTitle = new JLabel("登录成功");
        lblTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        lblTitle.setForeground(new Color(50, 50, 50));
        
        JLabel lblName = new JLabel("欢迎回来，" + name);
        lblName.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        lblName.setForeground(new Color(40, 40, 40));

        JLabel lblRole = new JLabel(String.format(
            "<html>您的系统身份：<span style='background-color:rgb(%d,%d,%d); padding:2px 8px; border-radius:4px;'>%s</span></html>", 
            markerColor.getRed(), markerColor.getGreen(), markerColor.getBlue(), role));
        lblRole.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        lblRole.setForeground(new Color(90, 90, 90));

        textPanel.add(lblTitle);
        textPanel.add(Box.createVerticalStrut(15));
        textPanel.add(lblName);
        textPanel.add(Box.createVerticalStrut(12)); 
        textPanel.add(lblRole);
        textPanel.add(Box.createVerticalStrut(12));

        panel.add(textPanel, BorderLayout.CENTER);

        JButton btnEnter = new JButton("进入系统");
        btnEnter.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        btnEnter.setBackground(BTN_GREEN);
        btnEnter.setForeground(new Color(40, 70, 40));
        btnEnter.setFocusPainted(false);
        btnEnter.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        btnEnter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEnter.addActionListener(e -> {
            dialog.dispose();
            this.dispose();
            SwingUtilities.invokeLater(() -> new MainFrame(loggedInUser).setVisible(true));
        });
        panel.add(btnEnter, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf()); } 
        catch (Exception ex) {}
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}