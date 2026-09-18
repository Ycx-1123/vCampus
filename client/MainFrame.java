package vCampus.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// ★ 新增导包：修复找不到 Message 类的报错 (Add import to fix "Cannot resolve symbol Message")
import vCampus.common.Message; 
// ★ 如果你的 LoginFrame 不在 vCampus.client 包下，请将下面这行的注释取消掉 (Uncomment below if LoginFrame is in another package)
// import vCampus.client.view.LoginFrame; 

import vCampus.common.vo.Admin;
import vCampus.common.vo.Student;
import vCampus.common.vo.Teacher;

public class MainFrame extends JFrame {
    private Object currentUser;
    private BankPanel bankPanel;
    
    // 核心布局组件 (Core layout components)
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private List<MenuButton> menuButtons = new ArrayList<>();

    // 6个模块的标记高亮色 (Marker highlight colors for the 6 modules)
    private static final Color COLOR_INFO    = new Color(243, 229, 245); // 浅紫 (Light Purple)
    private static final Color COLOR_CLASS   = new Color(255, 236, 179); // 浅橙 (Light Orange)
    private static final Color COLOR_LIBRARY = new Color(227, 242, 253); // 浅蓝 (Light Blue)
    private static final Color COLOR_DORM    = new Color(220, 237, 200); // 浅绿 (Light Green)
    private static final Color COLOR_SHOP    = new Color(252, 228, 236); // 浅粉 (Light Pink)
    private static final Color COLOR_BANK    = new Color(255, 249, 196); // 浅黄 (Light Yellow)

    public MainFrame(Object user) {
        this.currentUser = user;
        setTitle("vCampus - 虚拟校园平台");
        setSize(1050, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
    }

    private void initUI() { 
        setLayout(new BorderLayout());

        // ==========================================
        // 1. 侧边栏导航 (Sidebar Navigation - Pure White Background)
        // ==========================================
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230, 230, 230)));

        // 1.1 侧边栏顶部：Logo 和 用户信息 (Sidebar Top: Logo & User Info)
        JPanel profilePanel = new JPanel();
        profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));
        profilePanel.setBackground(Color.WHITE);
        profilePanel.setBorder(new EmptyBorder(30, 20, 20, 20));

        ImageIcon seuLogo = loadScaledIcon("seu_logo.png", 140, 45);
        JLabel lblLogo = seuLogo != null ? new JLabel(seuLogo) : new JLabel("vCampus");
        if (seuLogo == null) lblLogo.setFont(getUIFont(Font.BOLD, 24));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        String userName = extractName(currentUser);
        String userRole = extractRole(currentUser);

        JLabel lblName = new JLabel(userName);
        lblName.setFont(getUIFont(Font.BOLD, 18));
        lblName.setForeground(new Color(40, 40, 40));
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblRole = new JLabel(userRole);
        lblRole.setFont(getUIFont(Font.PLAIN, 13));
        lblRole.setForeground(new Color(120, 120, 120));
        lblRole.setAlignmentX(Component.CENTER_ALIGNMENT);

        profilePanel.add(lblLogo);
        profilePanel.add(Box.createVerticalStrut(25));
        profilePanel.add(lblName);
        profilePanel.add(Box.createVerticalStrut(5));
        profilePanel.add(lblRole);
        profilePanel.add(Box.createVerticalStrut(10));

        sidebar.add(profilePanel, BorderLayout.NORTH);

        // 1.2 侧边栏中部：模块菜单列表 (Sidebar Center: Module Menu List)
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(Color.WHITE);
        menuPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // 根据用户角色和权限动态加载模块 (Dynamically load modules based on user role and permissions)
        loadRoleBasedModules(menuPanel);

        sidebar.add(menuPanel, BorderLayout.CENTER);
        
        // 1.3 侧边栏底部：vCampus 品牌和退出登录按钮 (Sidebar Bottom: vCampus Brand & Logout Button - Centered)
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS)); 
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(new EmptyBorder(0, 0, 25, 0)); 
        
        JLabel lblBrand = new JLabel("vCampus");
        lblBrand.setFont(new Font("Arial", Font.BOLD, 22)); 
        lblBrand.setForeground(new Color(0, 89, 68)); 
        lblBrand.setAlignmentX(Component.CENTER_ALIGNMENT); 
        
        JButton btnLogout = new JButton("退出登录");
        btnLogout.setFont(getUIFont(Font.PLAIN, 12));
        btnLogout.setBackground(new Color(235, 235, 235)); 
        btnLogout.setForeground(new Color(80, 80, 80)); 
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.setAlignmentX(Component.CENTER_ALIGNMENT); 
        btnLogout.addActionListener(e -> {
        	// ★ 修改点 4：主动向服务端发送下线请求，并清空本地缓存
            sendLogoutRequest();
            SocketClient.currentUser = null; 
            
            this.dispose(); 
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true)); 
        });
        
        footerPanel.add(lblBrand);
        footerPanel.add(Box.createVerticalStrut(10)); 
        footerPanel.add(btnLogout);
        
        sidebar.add(footerPanel, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);
        
        // ★ 2. 新增：拦截窗口右上角的 X 关闭事件 (Add: Intercept window closing event)
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                sendLogoutRequest(); // 确保强制关闭时也释放状态 (Ensure status is released on force close)
            }
        });
        
        // ==========================================
        // 2. 右侧主要内容区域 (Right Main Content Area - CardLayout)
        // ==========================================
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(new Color(248, 250, 252));
        
        if (currentUser instanceof Student) {
            cardPanel.add(new StudentPanel((Student)currentUser), "INFO");
        } else if (currentUser instanceof Admin) {
            cardPanel.add(new AdminStudentPanel(), "INFO");
        }
        
        if (currentUser instanceof Student) {
            // 学生：显示学生选课面板
            cardPanel.add(new CoursePanel(currentUser), "CLASS");
        } else if (currentUser instanceof Teacher) {
            // 教师：显示教师管理面板（班级名单 + 成绩录入）
            cardPanel.add(new TeacherCoursePanel(currentUser), "CLASS");
        } else if (currentUser instanceof Admin) {
            // 管理员：显示管理员后台面板（增删改查 + 开放选课设置）
            cardPanel.add(new AdminCoursePanel(currentUser), "CLASS");
        }
        cardPanel.add(new LibraryPanel(currentUser), "LIBRARY");
        cardPanel.add(createPlaceholder("宿舍管理模块 - 建设中..."), "DORM");
        bankPanel = new BankPanel(currentUser);
        cardPanel.add(bankPanel, "BANK");
        
        if (currentUser instanceof Admin) {
            cardPanel.add(new AdminDormPanel((Admin)currentUser),"DORM");
        } else {
            cardPanel.add(new DormPanel(currentUser),"DORM");
        }
        
        // 创建商店用户对象并透传当前登录账户信息
        vCampus.common.shop.UserAccount shopUser = new vCampus.common.shop.UserAccount();
        if (currentUser instanceof Student) {
            Student student = (Student) currentUser;
            shopUser.setUserId(student.getSCard() != null ? student.getSCard() : "S001");
        } else {
            shopUser.setUserId("user001");
        }

        cardPanel.add(new ShopPanel(currentUser), "SHOP");

        add(cardPanel, BorderLayout.CENTER); 

        // 默认选中第一个可用的模块 (Select the first available module by default)
        if (!menuButtons.isEmpty()) {
            menuButtons.get(0).setSelected(true);
            cardLayout.show(cardPanel, menuButtons.get(0).getCardId());
        }
    }

    /**
     * 提取 FlatLaf 提供的原生系统高清字体，防止中文字体发虚或回退到宋体
     * (Extracts FlatLaf's native system HD font to prevent fuzzy text or fallback to SimSun)
     */
    private Font getUIFont(int style, float size) {
        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) {
            baseFont = UIManager.getFont("Label.font");
        }
        if (baseFont != null) {
            return baseFont.deriveFont(style, size);
        }
        return new Font("SansSerif", style, (int)size);
    }

    /**
     * 根据身份和权限加载左侧菜单 (Dynamically loads sidebar modules based on identity and permissions)
     */
    private void loadRoleBasedModules(JPanel menuPanel) {
        if (currentUser instanceof Teacher) {
            // Teacher: Only 4 modules, renamed to "上课管理"
            addMenuButton(menuPanel, "class_logo.jpg",   "上课管理", COLOR_CLASS,   "CLASS");
            addMenuButton(menuPanel, "library_logo.jpg", "图书馆",   COLOR_LIBRARY, "LIBRARY");
            addMenuButton(menuPanel, "shop_logo.jpg",    "校园商店", COLOR_SHOP,    "SHOP");
            addMenuButton(menuPanel, "bank_logo.jpg",    "校园银行", COLOR_BANK,    "BANK");
            
        } else if (currentUser instanceof Admin) {
            // Admin: Dynamically check boolean permissions
            Admin admin = (Admin) currentUser;
            if (admin.isManageStudent()) addMenuButton(menuPanel, "info_logo.jpg",    "学籍管理", COLOR_INFO,    "INFO");
            if (admin.isManageCourse())  addMenuButton(menuPanel, "class_logo.jpg",   "选课系统", COLOR_CLASS,   "CLASS");
            if (admin.isManageLibrary()) addMenuButton(menuPanel, "library_logo.jpg", "图书馆",   COLOR_LIBRARY, "LIBRARY");
            if (admin.isManageDorm())    addMenuButton(menuPanel, "dorm_logo.jpg",    "宿舍管理", COLOR_DORM,    "DORM");
            if (admin.isManageShop())    addMenuButton(menuPanel, "shop_logo.jpg",    "校园商店", COLOR_SHOP,    "SHOP");
            if (admin.isManageBank())    addMenuButton(menuPanel, "bank_logo.jpg",    "校园银行", COLOR_BANK,    "BANK");
            
        } else {
            // Student (Default): All 6 modules
            addMenuButton(menuPanel, "info_logo.jpg",    "信息门户", COLOR_INFO,    "INFO");
            addMenuButton(menuPanel, "class_logo.jpg",   "选课系统", COLOR_CLASS,   "CLASS");
            addMenuButton(menuPanel, "library_logo.jpg", "图书馆",   COLOR_LIBRARY, "LIBRARY");
            addMenuButton(menuPanel, "dorm_logo.jpg",    "宿舍管理", COLOR_DORM,    "DORM");
            addMenuButton(menuPanel, "shop_logo.jpg",    "校园商店", COLOR_SHOP,    "SHOP");
            addMenuButton(menuPanel, "bank_logo.jpg",    "校园银行", COLOR_BANK,    "BANK");
        }
    }

    /**
     * 生成左侧菜单项按钮的辅助方法 (Helper method to generate left menu items)
     */
    private void addMenuButton(JPanel parent, String iconName, String text, Color markerColor, String cardId) {
        MenuButton btn = new MenuButton(iconName, text, markerColor, cardId);
        menuButtons.add(btn);
        
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Exclusive selection logic
                for (MenuButton mb : menuButtons) mb.setSelected(false);
                btn.setSelected(true);
                cardLayout.show(cardPanel, cardId);

                // 每次进入校园银行时重新查询最新余额和交易流水
                if ("BANK".equals(cardId) && bankPanel != null) {
                    bankPanel.refresh();
                }
            }
        });
        
        parent.add(btn);
        parent.add(Box.createVerticalStrut(8)); 
    }

    /**
     * 自定义菜单按钮组件 (Custom menu button component)
     */
    private class MenuButton extends JPanel {
        private boolean isSelected = false;
        private JLabel lblText;
        private String cardId;

        public MenuButton(String iconName, String text, Color markerColor, String cardId) {
            this.cardId = cardId;
            setLayout(new FlowLayout(FlowLayout.LEFT, 15, 8)); 
            setBackground(Color.WHITE);
            setMaximumSize(new Dimension(240, 55));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            ImageIcon icon = loadScaledIcon(iconName, 32, 32);
            JLabel lblIcon = icon != null ? new JLabel(icon) : new JLabel("❖");
            lblIcon.setPreferredSize(new Dimension(32, 32));
            lblIcon.setHorizontalAlignment(SwingConstants.CENTER);

            lblText = new JLabel(text);
            // 采用动态获取的系统 UI 高清字体 (Use dynamically fetched system UI HD font)
            lblText.setFont(getUIFont(Font.BOLD, 16));
            lblText.setForeground(new Color(90, 90, 90));
            lblText.setPreferredSize(new Dimension(96, 32)); 
            lblText.setHorizontalAlignment(SwingConstants.CENTER); 
            lblText.setOpaque(true);
            lblText.setBackground(markerColor); 

            add(Box.createHorizontalStrut(15));
            add(lblIcon);
            add(lblText);

            // Hover feedback
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected) setBackground(new Color(250, 250, 250));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isSelected) setBackground(Color.WHITE);
                }
            });
        }

        public String getCardId() { return cardId; }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
            // 选中时的视觉反馈逻辑 (Visual logic for selection)
            if (selected) {
                setBackground(new Color(242, 242, 242));
                lblText.setForeground(Color.BLACK);
            } else {
                setBackground(Color.WHITE);
                lblText.setForeground(new Color(90, 90, 90));
            }
            repaint();
        }
    }

    // --- Utility Methods ---

    private ImageIcon loadScaledIcon(String filename, int width, int height) {
        String[] paths = { filename, "src/" + filename, "src/vCampus/client/" + filename };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) {
                Image img = new ImageIcon(path).getImage();
                return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
            }
        }
        return null;
    }

    private JPanel createPlaceholder(String text) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(248, 250, 252));
        JLabel l = new JLabel(text);
        l.setFont(getUIFont(Font.BOLD, 20));
        l.setForeground(new Color(150, 150, 150));
        p.add(l);
        return p;
    }

    private String extractName(Object user) {
        if (user instanceof Student) return ((Student) user).getSname() != null ? ((Student) user).getSname() : "伍诗文";
        if (user instanceof Teacher) return ((Teacher) user).getTeacherName();
        if (user instanceof Admin) return ((Admin) user).getAName();
        return "暂无"; 
    }

    private String extractRole(Object user) {
        if (user instanceof Student) return "学生 / Student";
        if (user instanceof Teacher) return "教师 / Teacher";
        if (user instanceof Admin) return "管理员 / Admin";
        return "暂无";
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        try { 
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf()); 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        // 创建有效的 Student 对象用于测试,调试时注释掉
        /*
        Student testStudent = new Student();
        testStudent.setSId("S2024001");
        testStudent.setSname("测试学生");
        testStudent.setStuCard("CARD001");
        testStudent.setEnrollYear("2024");
        testStudent.setClazzid("CLASS001");
        testStudent.setFinishedCredit(0.0);
        testStudent.setRequiredCredit(160.0);
        testStudent.setSgender("男");
        testStudent.setSphone("13800138000");
        testStudent.setSemail("test@seu.edu.cn");
        testStudent.setSaddress("江苏省南京市");
        testStudent.setLocked(false);
        testStudent.setCanEdit(true);
        
        SwingUtilities.invokeLater(() -> new MainFrame(testStudent).setVisible(true));
        */
    }
    
    // ★ 新增方法：发送下线请求 (New method: Send logout request)
    private void sendLogoutRequest() {
        String userId = extractCard(currentUser);
        if (userId != null && !userId.isEmpty()) {
            Message logoutMsg = new Message("LOGOUT", userId);
            logoutMsg.setSenderId(userId);
            // 这里调用你的 SocketClient 发送消息 (Call your SocketClient to send)
            SocketClient.send(logoutMsg); 
        }
    }

    // ★ 新增方法：提取一卡通号 (New method: Extract card ID)
    private String extractCard(Object user) {
        if (user instanceof Student) return ((Student) user).getSCard();
        if (user instanceof Teacher) return ((Teacher) user).getTeacherCard();
        if (user instanceof Admin) return ((Admin) user).getACard();
        return null;
    }
}