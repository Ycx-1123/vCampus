package vCampus.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vCampus.common.Message;
import vCampus.common.consts.MsgConst;
import vCampus.common.vo.Admin;
import vCampus.common.vo.Course;
import vCampus.common.vo.CourseOffering;
import vCampus.common.vo.Program;
import vCampus.common.vo.ProgramCourse;
import vCampus.common.vo.SelectRound;
import vCampus.common.vo.Teacher;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
public class AdminCoursePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private Object currentUser;
    private String adminId;
    private javax.swing.Timer roundTimer;
    private javax.swing.Timer pollTimer;
    private static final int POLL_INTERVAL_MS = 5000;
    private static final Color BG_COLOR = new Color(242, 250, 253);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(216, 237, 230);
    private static final Color BTN_GREEN = new Color(183, 220, 187);
    private static final Color BTN_BLUE = new Color(180, 210, 240);
    private static final Color BTN_DANGER = new Color(240, 180, 180);
    private static final Color HEADER_BG = new Color(235, 245, 240);
    private static final Color MENU_SELECTED = new Color(215, 235, 225);

    private JPanel contentPanel;
    private CardLayout contentCardLayout;
    private List<MenuButton> menuButtons = new ArrayList<>();

    private DefaultTableModel courseListModel;
    private DefaultTableModel availableCoursesModel;
    private DefaultTableModel selectedCoursesModel;

    private JTextField txtSearchStudent;
    private JPopupMenu studentPopup;
    private JList<String> studentSuggestionList;
    private DefaultListModel<String> suggestionModel;
    private List<Map<String, Object>> suggestionData;
    private JLabel lblCurrentStudent;
    private String currentStudentId;
    private String currentStudentName;

    private SelectRound currentRound;
    private JLabel lblCurrentRoundHeader;
    private JPanel roundsPanel;

    public AdminCoursePanel(Object user) {
        this.currentUser = user;
        if (user instanceof Admin) this.adminId = ((Admin) user).getAId();
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        initUI();
        updateHeaderRound();
        loadCourseList();
        startAdminPolling(); 
    }

    // ============================================================
    // 初始化
    // ============================================================
    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 25, 10, 25)));

        JLabel lblTitle = new JLabel("选课系统管理");
        lblTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        lblTitle.setForeground(new Color(40, 60, 50));
        header.add(lblTitle, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setBackground(CARD_BG);
        lblCurrentRoundHeader = new JLabel("当前轮次：加载中...");
        lblCurrentRoundHeader.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        rightPanel.add(lblCurrentRoundHeader);

        String userName = currentUser instanceof Admin ? ((Admin) currentUser).getAName() : "管理员";
        JLabel lblUser = new JLabel("管理员：" + userName);
        lblUser.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblUser.setForeground(new Color(100, 100, 100));
        rightPanel.add(lblUser);
        header.add(rightPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG_COLOR);
        main.add(createLeftMenu(), BorderLayout.WEST);

        contentCardLayout = new CardLayout();
        contentPanel = new JPanel(contentCardLayout);
        contentPanel.setBackground(BG_COLOR);
        contentPanel.add(createCourseListPanel(), "courses");
        contentPanel.add(createSelectPanel(), "select");
        contentPanel.add(createRoundPanel(), "round");
        main.add(contentPanel, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        if (!menuButtons.isEmpty()) {
            menuButtons.get(0).setSelected(true);
            contentCardLayout.show(contentPanel, "courses");
        }
    }

    private JPanel createLeftMenu() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD_BG);
        panel.setPreferredSize(new Dimension(160, getHeight()));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR),
                new EmptyBorder(10, 0, 10, 0)));

        panel.add(createGroupLabel("课程管理"));
        addMenuItem(panel, "课程列表", "courses");
        panel.add(Box.createVerticalStrut(12));
        panel.add(createGroupLabel("选课管理"));
        addMenuItem(panel, "选课管理", "select");
        addMenuItem(panel, "选课轮次", "round");
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JLabel createGroupLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        label.setForeground(new Color(120, 140, 130));
        label.setBorder(new EmptyBorder(8, 20, 5, 15));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void addMenuItem(JPanel parent, String text, String cardId) {
        MenuButton btn = new MenuButton(text);
        menuButtons.add(btn);
        btn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                for (MenuButton mb : menuButtons) mb.setSelected(false);
                btn.setSelected(true);
                contentCardLayout.show(contentPanel, cardId);
                if ("courses".equals(cardId)) loadCourseList();
                if ("round".equals(cardId)) loadRounds();
            }
        });
        parent.add(btn);
        parent.add(Box.createVerticalStrut(1));
    }

    private class MenuButton extends JPanel {
        private boolean selected = false;
        private JLabel lbl;

        public MenuButton(String text) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 20, 8));
            setBackground(CARD_BG);
            setMaximumSize(new Dimension(160, 34));
            setPreferredSize(new Dimension(160, 34));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            lbl = new JLabel(text);
            lbl.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
            lbl.setForeground(new Color(60, 80, 70));
            add(lbl);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (!selected) setBackground(new Color(245, 250, 248)); }
                public void mouseExited(MouseEvent e) { if (!selected) setBackground(CARD_BG); }
            });
        }

        public void setSelected(boolean s) {
            this.selected = s;
            setBackground(s ? MENU_SELECTED : CARD_BG);
            lbl.setFont(new Font("Microsoft YaHei", s ? Font.BOLD : Font.PLAIN, 13));
            repaint();
        }
    }

    // ============================================================
    // 校验工具
    // ============================================================
    private String normalizeSemester(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.matches("\\d{4}-\\d{4}-[12]")) return s;
        return null;
    }

    private boolean isValidSchedule(String s) {
        if (s == null || s.isEmpty()) return true;
        return s.matches("^周[一二三四五六日天]\\s*第?\\d+(-\\d+)?节.*$");
    }

    private boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    // ============================================================
    // 1. 课程列表
    // ============================================================
    private JPanel createCourseListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBackground(CARD_BG);
        toolbar.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        toolbar.add(new JLabel("关键词："));
        JTextField txtSearch = new JTextField(14);
        toolbar.add(txtSearch);

        JButton btnSearch = new JButton("搜索");
        btnSearch.setBackground(BTN_BLUE);
        btnSearch.setBorder(new EmptyBorder(4, 14, 4, 14));
        btnSearch.addActionListener(e -> {
            String kw = txtSearch.getText().trim();
            if (kw.isEmpty()) loadCourseList(); else searchCourses(kw);
        });
        toolbar.add(btnSearch);

        JButton btnAdd = new JButton("新增课程");
        btnAdd.setBackground(BTN_GREEN);
        btnAdd.setBorder(new EmptyBorder(4, 14, 4, 14));
        btnAdd.addActionListener(e -> showAddCourseDialog());
        toolbar.add(btnAdd);

        JButton btnBatchAdd = new JButton("批量新增课程");
        btnBatchAdd.setBackground(new Color(200, 230, 200));
        btnBatchAdd.setBorder(new EmptyBorder(4, 14, 4, 14));
        btnBatchAdd.addActionListener(e -> showBatchAddCourseDialog());
        toolbar.add(btnBatchAdd);

        JButton btnExportTpl = new JButton("导出课表模板");
        btnExportTpl.setBackground(new Color(200, 220, 240));
        btnExportTpl.setBorder(new EmptyBorder(4, 14, 4, 14));
        btnExportTpl.addActionListener(e -> exportOfferingTemplate());
        toolbar.add(btnExportTpl);

        JButton btnImportExcel = new JButton("导入课表Excel");
        btnImportExcel.setBackground(new Color(220, 230, 180));
        btnImportExcel.setBorder(new EmptyBorder(4, 14, 4, 14));
        btnImportExcel.addActionListener(e -> importOfferingExcel());
        toolbar.add(btnImportExcel);

        panel.add(toolbar, BorderLayout.NORTH);

        String[] cols = {"课程号", "课程名", "学分", "教师", "容量", "已选", "状态", "开课ID", "课程详情", "修改", "删除"};
        courseListModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(courseListModel);
        table.setRowHeight(32);
        table.getTableHeader().setBackground(HEADER_BG);
        table.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        TableColumn col = table.getColumnModel().getColumn(7);
        col.setMinWidth(0); col.setMaxWidth(0); col.setPreferredWidth(0);

        table.getColumnModel().getColumn(8).setCellRenderer(new ButtonRenderer("课程详情"));
        table.getColumnModel().getColumn(9).setCellRenderer(new ButtonRenderer("修改"));
        table.getColumnModel().getColumn(10).setCellRenderer(new ButtonRenderer("删除"));
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0) return;
                Object idObj = table.getValueAt(row, 7);
                String offeringId = idObj != null ? idObj.toString() : null;
                if (offeringId == null || offeringId.isEmpty()) return;
                if (col == 8) showCourseDetailDialog(offeringId);
                else if (col == 9) showEditCourseDialog(offeringId);
                else if (col == 10) deleteCourse(offeringId);
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private void loadCourseList() {
        courseListModel.setRowCount(0);
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", null);
        params.put("roundId", currentRound != null ? currentRound.getRoundId() : null);
        fillCourseTable(params);
    }

    private void searchCourses(String keyword) {
        courseListModel.setRowCount(0);
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("roundId", currentRound != null ? currentRound.getRoundId() : null);
        fillCourseTable(params);
    }

    @SuppressWarnings("unchecked")
    private void fillCourseTable(Map<String, Object> params) {
        try {
            Message request = new Message();
            request.setType(MsgConst.ADMIN_GET_ALL_COURSES);
            request.setData(params);
            Message response = SocketClient.send(request);

            if (response != null && response.isSuccess() && response.getData() != null) {
                List<Map<String, Object>> courses = (List<Map<String, Object>>) response.getData();
                for (Map<String, Object> row : courses) {
                    String teacherName = str(row.get("teacherName"));
                    String schedule = str(row.get("schedule"));
                    String classroom = str(row.get("classroom"));

                    boolean incomplete = teacherName.isEmpty() || schedule.isEmpty() || classroom.isEmpty();
                    String teacherDisplay = teacherName.isEmpty() ? "（待填写）" : teacherName;
                    String statusDisplay = incomplete ? "待完善" : str(row.get("status"));

                    String enrolled = str(row.get("enrolledCount"));
                    String cap = str(row.get("capacity"));
                    String roomCap = str(row.get("roomCapacity"));
                    String capDisplay = enrolled + "/" + cap + " (" + roomCap + ")";

                    courseListModel.addRow(new Object[]{
                            row.get("courseId"), row.get("courseName"), row.get("credit"),
                            teacherDisplay, capDisplay, row.get("enrolledCount"),
                            statusDisplay, row.get("offeringId"), "课程详情"
                    });
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ============================================================
    // 修改课程对话框
    // ============================================================
    @SuppressWarnings("unchecked")
    private void showEditCourseDialog(String offeringId) {
        Map<String, Object> detail = fetchCourseDetail(offeringId);
        if (detail == null) {
            JOptionPane.showMessageDialog(this, "获取课程信息失败", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (detail.containsKey("data") && detail.get("data") instanceof Map) {
            detail = (Map<String, Object>) detail.get("data");
        }
        final Map<String, Object> finalDetail = detail;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "修改开课班级", true);
        dialog.setSize(500, 760);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;
        gbc.gridx = 0; gbc.gridy = r++; gbc.gridwidth = 2;
        form.add(sectionLabel("课程基本信息"), gbc);
        gbc.gridwidth = 1;

        JLabel lblCourseId = readOnlyLabel(str(detail.get("courseId")));
        JLabel lblCourseName = readOnlyLabel(str(detail.get("courseName")));
        JLabel lblCredit = readOnlyLabel(str(detail.get("credit")));
        JLabel lblHours = readOnlyLabel(str(detail.get("hours")));
        JLabel lblCategoryId = readOnlyLabel(str(detail.get("categoryId")));
        JLabel lblDescription = readOnlyLabel(str(detail.get("description")));

        gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("课程号："), gbc);
        gbc.gridx = 1; form.add(lblCourseId, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("课程名："), gbc);
        gbc.gridx = 1; form.add(lblCourseName, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("学分："), gbc);
        gbc.gridx = 1; form.add(lblCredit, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("学时："), gbc);
        gbc.gridx = 1; form.add(lblHours, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("类别ID："), gbc);
        gbc.gridx = 1; form.add(lblCategoryId, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("课程描述："), gbc);
        gbc.gridx = 1; form.add(lblDescription, gbc);

        r++; gbc.gridx = 0; gbc.gridy = r++; gbc.gridwidth = 2;
        form.add(sectionLabel("开课班级信息"), gbc);
        gbc.gridwidth = 1;

        JTextField txtTeacherId = new JTextField(18);
        JTextField txtTeacherName = new JTextField(18);
        JTextField txtSemester = new JTextField(18);
        JTextField txtCapacity = new JTextField(18);
        JTextField txtRoomCapacity = new JTextField(18);
        JTextField txtSchedule = new JTextField(18);
        JTextField txtClassroom = new JTextField(18);
        JComboBox<String> cbCampus = new JComboBox<>(new String[]{"九龙湖", "四牌楼", "丁家桥"});

        txtTeacherId.setText(str(detail.get("teacherId")));
        txtTeacherName.setText(str(detail.get("teacherName")));
        txtSemester.setText(str(detail.get("semester")));
        String capStr = str(detail.get("capacity"));
        txtCapacity.setText(capStr.isEmpty() ? "50" : capStr);
        String rcStr = str(detail.get("roomCapacity"));
        txtRoomCapacity.setText(rcStr.isEmpty() ? "70" : rcStr);
        txtSchedule.setText(str(detail.get("schedule")));
        txtClassroom.setText(str(detail.get("classroom")));
        String campusVal = str(detail.get("campus"));
        if (!campusVal.isEmpty()) cbCampus.setSelectedItem(campusVal);

        JLabel lblScheduleHint = hintLabel();

        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"open", "full", "closed"});
        String curStatus = str(detail.get("status"));
        if (!curStatus.isEmpty()) cbStatus.setSelectedItem(curStatus);

        JLabel lblTeacherHint = hintLabel();

        gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("教师一卡通号："), gbc);
        gbc.gridx = 1; form.add(txtTeacherId, gbc);
        r++; gbc.gridx = 1; gbc.gridy = r; form.add(lblTeacherHint, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("教师姓名："), gbc);
        gbc.gridx = 1; form.add(txtTeacherName, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("学期："), gbc);
        gbc.gridx = 1; form.add(txtSemester, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("容量："), gbc);
        gbc.gridx = 1; form.add(txtCapacity, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("教室容量："), gbc);
        gbc.gridx = 1; form.add(txtRoomCapacity, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("上课时间："), gbc);
        gbc.gridx = 1; form.add(txtSchedule, gbc);
        r++; gbc.gridx = 1; gbc.gridy = r; form.add(lblScheduleHint, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("教室："), gbc);
        gbc.gridx = 1; form.add(txtClassroom, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("校区："), gbc);
        gbc.gridx = 1; form.add(cbCampus, gbc);
        r++; gbc.gridx = 0; gbc.gridy = r; form.add(new JLabel("状态："), gbc);
        gbc.gridx = 1; form.add(cbStatus, gbc);

        Runnable queryTeacher = () -> {
            String tid = txtTeacherId.getText().trim();
            if (tid.isEmpty()) { lblTeacherHint.setText(" "); return; }
            Teacher t = queryTeacherFromServer(tid);
            if (t != null) {
                txtTeacherName.setText(t.getTeacherName() != null ? t.getTeacherName() : "");
                lblTeacherHint.setText("已找到教师：" + t.getTeacherName());
                lblTeacherHint.setForeground(new Color(60, 150, 80));
            } else {
                lblTeacherHint.setText("未找到该一卡通号对应的教师");
                lblTeacherHint.setForeground(new Color(200, 80, 80));
            }
        };
        SwingUtilities.invokeLater(queryTeacher);
        txtTeacherId.addActionListener(e -> queryTeacher.run());
        txtTeacherId.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { queryTeacher.run(); }
        });

        txtSchedule.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String s = txtSchedule.getText().trim();
                if (s.isEmpty()) { lblScheduleHint.setText(" "); return; }
                if (isValidSchedule(s)) {
                    lblScheduleHint.setText("格式正确");
                    lblScheduleHint.setForeground(new Color(60, 150, 80));
                } else {
                    lblScheduleHint.setText("格式错误，应为 周一 第1-2节");
                    lblScheduleHint.setForeground(Color.RED);
                }
            }
        });

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        dialog.add(formScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(Color.WHITE);

        JButton btnOk = new JButton("保存修改");
        btnOk.setBackground(BTN_GREEN);
        btnOk.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnOk.addActionListener(e -> {
            try {
                String sem = normalizeSemester(txtSemester.getText().trim());
                if (sem == null) {
                    JOptionPane.showMessageDialog(dialog, "学期格式错误，应为 yyyy-yyyy-N（如 2025-2026-1）");
                    return;
                }
                if (!isValidSchedule(txtSchedule.getText().trim())) {
                    lblScheduleHint.setText("格式错误，应为 周一 第1-2节");
                    lblScheduleHint.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(dialog, "上课时间格式错误，应为 周一 第1-2节");
                    return;
                }

                int capacity = (int) parseDoubleSafe(txtCapacity.getText().trim(), 50);
                int roomCapacity = (int) parseDoubleSafe(txtRoomCapacity.getText().trim(), 70);
                if (capacity < 1 || capacity > 500) {
                    JOptionPane.showMessageDialog(dialog, "课程容量应在 1 ~ 500 之间");
                    return;
                }
                if (roomCapacity < 1 || roomCapacity > 500) {
                    JOptionPane.showMessageDialog(dialog, "教室容量应在 1 ~ 500 之间");
                    return;
                }
                if (roomCapacity < capacity) {
                    JOptionPane.showMessageDialog(dialog, "教室容量不能小于课程容量");
                    return;
                }

                Course course = new Course();
                course.setCourseId(str(finalDetail.get("courseId")));
                course.setCourseName(str(finalDetail.get("courseName")));
                course.setCredit(parseDoubleSafe(str(finalDetail.get("credit")), 0));
                course.setHours((int) parseDoubleSafe(str(finalDetail.get("hours")), 0));
                course.setCategoryId(str(finalDetail.get("categoryId")));
                course.setDescription(str(finalDetail.get("description")));

                CourseOffering offering = new CourseOffering();
                offering.setOfferingId(offeringId);
                offering.setCourseId(str(finalDetail.get("courseId")));
                offering.setTeacherId(txtTeacherId.getText().trim());
                offering.setTeacherName(txtTeacherName.getText().trim());
                offering.setSemester(sem);
                offering.setCapacity(capacity);
                offering.setRoomCapacity(roomCapacity);
                offering.setSchedule(txtSchedule.getText().trim());
                offering.setClassroom(txtClassroom.getText().trim());
                offering.setCampus((String) cbCampus.getSelectedItem());
                offering.setStatus((String) cbStatus.getSelectedItem());

                String result = updateCourseAndOfferingToServer(course, offering);
                if ("修改成功".equals(result)) {
                    JOptionPane.showMessageDialog(dialog, "修改成功");
                    dialog.dispose();
                    loadCourseList();
                } else {
                    JOptionPane.showMessageDialog(dialog, result, "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "保存失败：" + ex.getMessage());
            }
        });
        btnPanel.add(btnOk);

        JButton btnCancel = new JButton("取消");
        btnCancel.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnCancel);

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void deleteCourse(String offeringId) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要删除开课记录 " + offeringId + " 吗？\n该操作会同时删除相关选课记录，不可恢复。",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Message request = new Message();
            request.setType(MsgConst.ADMIN_DELETE_OFFERING);
            request.setData(offeringId);
            Message response = SocketClient.send(request);
            if (response != null && response.isSuccess()) {
                JOptionPane.showMessageDialog(this, "删除成功");
                loadCourseList();
            } else {
                JOptionPane.showMessageDialog(this, "删除失败: " + (response != null ? response.getResponseMsg() : ""), "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private JLabel readOnlyLabel(String text) {
        JLabel lbl = new JLabel(text != null ? text : "");
        lbl.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lbl.setForeground(new Color(40, 40, 40));
        lbl.setBorder(new EmptyBorder(3, 2, 3, 2));
        lbl.setFocusable(false);
        lbl.setRequestFocusEnabled(false);
        lbl.setCursor(Cursor.getDefaultCursor());
        return lbl;
    }

    private String updateCourseAndOfferingToServer(Course course, CourseOffering offering) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("course", course);
            params.put("offering", offering);
            Message req = new Message();
            req.setType(MsgConst.ADMIN_UPDATE_COURSE_AND_OFFERING);
            req.setData(params);
            Message resp = SocketClient.send(req);
            return resp.getResponseMsg();
        } catch (Exception e) {
            e.printStackTrace();
            return "连接服务器失败：" + e.getMessage();
        }
    }

    // ============================================================
    // 课程详情弹窗
    // ============================================================
    @SuppressWarnings("unchecked")
    private void showCourseDetailDialog(String offeringId) {
        Map<String, Object> detailData = fetchCourseDetail(offeringId);
        if (detailData == null) {
            JOptionPane.showMessageDialog(this, "获取课程详情失败", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (detailData.containsKey("data") && detailData.get("data") instanceof Map) {
            detailData = (Map<String, Object>) detailData.get("data");
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "课程详情", true);
        dialog.setSize(900, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(new Color(240, 248, 255));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(15, 20, 15, 20)));

        String statusText = str(detailData.get("status"));
        if ("open".equals(statusText)) statusText = "open（开放）";
        else if ("closed".equals(statusText)) statusText = "closed（已关闭）";
        else if ("full".equals(statusText)) statusText = "full（已满）";

        String[][] items = {
                {"课程号：", str(detailData.get("courseId"))},
                {"课程名：", str(detailData.get("courseName"))},
                {"学分：", str(detailData.get("credit"))},
                {"学时：", str(detailData.get("hours"))},
                {"课程类别：", str(detailData.get("categoryName"))},
                {"课程描述：", str(detailData.get("description"))},
                {"任课教师：", str(detailData.get("teacherName"))},
                {"容量：", str(detailData.get("capacity"))},
                {"教室容量：", str(detailData.get("roomCapacity"))},
                {"已选人数：", str(detailData.get("enrolledCount"))},
                {"上课时间：", str(detailData.get("schedule"))},
                {"教室：", str(detailData.get("classroom"))},
                {"学期：", str(detailData.get("semester"))},
                {"状态：", statusText}
        };

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < items.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            JLabel k = new JLabel(items[i][0]);
            k.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            k.setForeground(new Color(60, 80, 70));
            infoPanel.add(k, gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            JLabel v = new JLabel(items[i][1]);
            v.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            infoPanel.add(v, gbc);
        }

        JScrollPane infoScroll = new JScrollPane(infoPanel);
        infoScroll.setBorder(null);
        infoScroll.setPreferredSize(new Dimension(850, 280));
        dialog.add(infoScroll, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBackground(Color.WHITE);
        JTextField txtStuSearch = new JTextField(16);
        JButton btnStuSearch = new JButton("搜索");
        btnStuSearch.setBackground(BTN_BLUE);
        btnStuSearch.setBorder(new EmptyBorder(4, 14, 4, 14));
        searchPanel.add(new JLabel("搜索学生："));
        searchPanel.add(txtStuSearch);
        searchPanel.add(btnStuSearch);
        centerPanel.add(searchPanel, BorderLayout.NORTH);

        String[] stuCols = {"学号", "姓名", "专业", "首修/重修", "成绩", "状态", "保存"};
        DefaultTableModel stuModel = new DefaultTableModel(stuCols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        JTable stuTable = new JTable(stuModel);
        stuTable.setRowHeight(30);
        stuTable.getTableHeader().setBackground(HEADER_BG);
        stuTable.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        stuTable.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        stuTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer("保存"));

        final String oid = offeringId;
        stuTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = stuTable.rowAtPoint(e.getPoint());
                int col = stuTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 6) {
                    if (stuTable.isEditing()) stuTable.getCellEditor().stopCellEditing();
                    String sid = (String) stuTable.getValueAt(row, 0);
                    Object so = stuTable.getValueAt(row, 4);
                    String ss = so != null ? so.toString().trim() : "";
                    if (ss.isEmpty()) { JOptionPane.showMessageDialog(dialog, "请输入成绩"); return; }
                    try {
                        double score = Double.parseDouble(ss);
                        if (score < 0 || score > 100) { JOptionPane.showMessageDialog(dialog, "成绩必须在 0-100 之间"); return; }
                        if (saveStudentScore(oid, sid, score)) {
                            JOptionPane.showMessageDialog(dialog, "成绩保存成功");
                            refreshStudentTable(oid, stuModel, txtStuSearch.getText().trim());
                        } else {
                            JOptionPane.showMessageDialog(dialog, "保存失败");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(dialog, "请输入有效数字");
                    }
                }
            }
        });

        JScrollPane stuScroll = new JScrollPane(stuTable);
        stuScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        centerPanel.add(stuScroll, BorderLayout.CENTER);
        dialog.add(centerPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(10, 15, 15, 15));
        JButton btnClose = new JButton("关闭");
        btnClose.setBackground(new Color(200, 200, 200));
        btnClose.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnClose.addActionListener(e -> dialog.dispose());
        bottom.add(btnClose);
        dialog.add(bottom, BorderLayout.SOUTH);

        refreshStudentTable(offeringId, stuModel, "");
        btnStuSearch.addActionListener(e -> refreshStudentTable(oid, stuModel, txtStuSearch.getText().trim()));
        dialog.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void refreshStudentTable(String offeringId, DefaultTableModel model, String keyword) {
        model.setRowCount(0);
        try {
            Message request = new Message();
            request.setType(MsgConst.ADMIN_GET_COURSE_DETAIL);
            request.setData(offeringId);
            Message response = SocketClient.send(request);
            if (response != null && response.isSuccess() && response.getData() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getData();
                Object obj = data.get("students");
                if (obj instanceof List) {
                    List<vCampus.common.vo.StudentScoreDTO> list = (List<vCampus.common.vo.StudentScoreDTO>) obj;
                    for (vCampus.common.vo.StudentScoreDTO s : list) {
                        if (keyword != null && !keyword.isEmpty()) {
                            String k = keyword.toLowerCase();
                            String sid = s.getStudentId() == null ? "" : s.getStudentId().toLowerCase();
                            String sn = s.getStudentName() == null ? "" : s.getStudentName().toLowerCase();
                            String mj = s.getMajor() == null ? "" : s.getMajor().toLowerCase();
                            if (!sid.contains(k) && !sn.contains(k) && !mj.contains(k)) continue;
                        }
                        model.addRow(new Object[]{
                                s.getStudentId(), s.getStudentName(), s.getMajor(),
                                "first".equals(s.getSelectType()) ? "首修" : "重修",
                                s.getScore() != null ? s.getScore() : "",
                                s.getStatus(), "保存"
                        });
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchCourseDetail(String offeringId) {
        try {
            Message request = new Message();
            request.setType(MsgConst.ADMIN_GET_COURSE_DETAIL);
            request.setData(offeringId);
            Message response = SocketClient.send(request);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return (Map<String, Object>) response.getData();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private boolean saveStudentScore(String offeringId, String studentId, double score) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("offeringId", offeringId);
            params.put("studentId", studentId);
            params.put("score", score);
            Message request = new Message();
            request.setType(MsgConst.TEACHER_ENTER_SCORE);
            request.setData(params);
            Message response = SocketClient.send(request);
            return response != null && response.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============================================================
    // 新增课程对话框
    // ============================================================
    private void showAddCourseDialog() {
        if (currentRound == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个选课轮次", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "新增课程与开课", true);
        dialog.setSize(500, 760);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        form.add(sectionLabel("课程基本信息"), gbc);
        gbc.gridwidth = 1;

        JTextField txtCourseId = new JTextField(18);
        JTextField txtCourseName = new JTextField(18);
        JTextField txtCredit = new JTextField(18);
        JTextField txtHours = new JTextField(18);
        JTextField txtCategoryId = new JTextField(18);
        JTextArea txtDescription = new JTextArea(2, 18);
        txtDescription.setLineWrap(true);
        JLabel lblCourseHint = hintLabel();
        JLabel lblCreditHint = hintLabel();

        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("课程号："), gbc);
        gbc.gridx = 1; form.add(txtCourseId, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; form.add(lblCourseHint, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("课程名："), gbc);
        gbc.gridx = 1; form.add(txtCourseName, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("学分（0.25 ~ 10）："), gbc);
        gbc.gridx = 1; form.add(txtCredit, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; form.add(lblCreditHint, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("学时："), gbc);
        gbc.gridx = 1; form.add(txtHours, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("类别ID："), gbc);
        gbc.gridx = 1; form.add(txtCategoryId, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("课程描述："), gbc);
        gbc.gridx = 1;
        JScrollPane dsp = new JScrollPane(txtDescription);
        dsp.setPreferredSize(new Dimension(220, 50));
        form.add(dsp, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        form.add(sectionLabel("开课班级信息"), gbc);
        gbc.gridwidth = 1;

        JTextField txtOfferingId = new JTextField(18);
        txtOfferingId.setEditable(false);
        txtOfferingId.setBackground(new Color(245, 245, 245));
        txtOfferingId.setToolTipText("系统自动生成");

        JTextField txtTeacherId = new JTextField(18);
        JTextField txtTeacherName = new JTextField(18);
        JTextField txtSemester = new JTextField(18);
        txtSemester.setText(currentRound.getSemester());
        JTextField txtCapacity = new JTextField("50", 18);
        JTextField txtRoomCapacity = new JTextField("70", 18);
        JTextField txtSchedule = new JTextField(18);
        JTextField txtClassroom = new JTextField(18);
        JComboBox<String> cbCampus = new JComboBox<>(new String[]{"九龙湖", "四牌楼", "丁家桥"});
        JLabel lblTeacherHint = hintLabel();
        JLabel lblScheduleHint = hintLabel();

        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("开课ID（自动）："), gbc);
        gbc.gridx = 1; form.add(txtOfferingId, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("教师一卡通号："), gbc);
        gbc.gridx = 1; form.add(txtTeacherId, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; form.add(lblTeacherHint, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("教师姓名："), gbc);
        gbc.gridx = 1; form.add(txtTeacherName, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("学期："), gbc);
        gbc.gridx = 1; form.add(txtSemester, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("容量（1 ~ 500）："), gbc);
        gbc.gridx = 1; form.add(txtCapacity, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("教室容量（1 ~ 500）："), gbc);
        gbc.gridx = 1; form.add(txtRoomCapacity, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("上课时间："), gbc);
        gbc.gridx = 1; form.add(txtSchedule, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; form.add(lblScheduleHint, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("教室："), gbc);
        gbc.gridx = 1; form.add(txtClassroom, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("校区："), gbc);
        gbc.gridx = 1; form.add(cbCampus, gbc);

        Runnable queryCourse = () -> {
            String cid = txtCourseId.getText().trim();
            if (cid.isEmpty()) { txtOfferingId.setText(""); return; }
            Course c = queryCourseFromServer(cid);
            if (c != null) {
                txtCourseName.setText(c.getCourseName() != null ? c.getCourseName() : "");
                txtCredit.setText(c.getCredit() > 0 ? String.valueOf(c.getCredit()) : "");
                txtHours.setText(c.getHours() > 0 ? String.valueOf(c.getHours()) : "");
                txtCategoryId.setText(c.getCategoryId() != null ? c.getCategoryId() : "");
                txtDescription.setText(c.getDescription() != null ? c.getDescription() : "");
                lblCourseHint.setText("该课程已存在，已自动填充");
                lblCourseHint.setForeground(new Color(60, 150, 80));
            } else {
                lblCourseHint.setText("新课程，请手动填写");
                lblCourseHint.setForeground(new Color(150, 120, 60));
            }
            String semester = txtSemester.getText().trim();
            if (semester.isEmpty()) { txtOfferingId.setText(""); return; }
            txtOfferingId.setText(generateOfferingId(cid, semester));
        };
        txtCourseId.addActionListener(e -> queryCourse.run());
        txtCourseId.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { queryCourse.run(); }
        });

        Runnable queryTeacher = () -> {
            String tid = txtTeacherId.getText().trim();
            if (tid.isEmpty()) return;
            Teacher t = queryTeacherFromServer(tid);
            if (t != null) {
                txtTeacherName.setText(t.getTeacherName() != null ? t.getTeacherName() : "");
                lblTeacherHint.setText("已找到教师：" + t.getTeacherName());
                lblTeacherHint.setForeground(new Color(60, 150, 80));
            } else {
                lblTeacherHint.setText("未找到该一卡通号对应的教师");
                lblTeacherHint.setForeground(new Color(200, 80, 80));
            }
        };
        txtTeacherId.addActionListener(e -> queryTeacher.run());
        txtTeacherId.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { queryTeacher.run(); }
        });

        txtCredit.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String s = txtCredit.getText().trim();
                if (s.isEmpty()) { lblCreditHint.setText(" "); return; }
                try {
                    double v = Double.parseDouble(s);
                    if (v < 0.25 || v > 10) {
                        lblCreditHint.setText("学分应在 0.25 ~ 10 之间");
                        lblCreditHint.setForeground(Color.RED);
                    } else {
                        lblCreditHint.setText(" ");
                    }
                } catch (NumberFormatException ex) {
                    lblCreditHint.setText("请输入数字");
                    lblCreditHint.setForeground(Color.RED);
                }
            }
        });

        txtSchedule.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String s = txtSchedule.getText().trim();
                if (s.isEmpty()) { lblScheduleHint.setText(" "); return; }
                if (isValidSchedule(s)) {
                    lblScheduleHint.setText("格式正确");
                    lblScheduleHint.setForeground(new Color(60, 150, 80));
                } else {
                    lblScheduleHint.setText("格式错误，应为 周一 第1-2节");
                    lblScheduleHint.setForeground(Color.RED);
                }
            }
        });

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        dialog.add(formScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(Color.WHITE);

        JButton btnOk = new JButton("确定");
        btnOk.setBackground(BTN_GREEN);
        btnOk.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnOk.addActionListener(e -> {
            try {
                if (txtCourseId.getText().trim().isEmpty() || txtCourseName.getText().trim().isEmpty()
                        || txtTeacherId.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "请填写完整信息");
                    return;
                }
                if (txtOfferingId.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "开课ID生成失败，请检查课程号和学期");
                    return;
                }

                String sem = normalizeSemester(txtSemester.getText().trim());
                if (sem == null) {
                    JOptionPane.showMessageDialog(dialog, "学期格式错误，应为 yyyy-yyyy-N（如 2025-2026-1）");
                    return;
                }

                double credit;
                try {
                    credit = Double.parseDouble(txtCredit.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "学分必须是数字");
                    return;
                }
                if (!inRange(credit, 0.25, 10)) {
                    JOptionPane.showMessageDialog(dialog, "学分应在 0.25 ~ 10 之间");
                    return;
                }

                int hours;
                try {
                    hours = Integer.parseInt(txtHours.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "学时必须是整数");
                    return;
                }
                if (!inRange(hours, 1, 500)) {
                    JOptionPane.showMessageDialog(dialog, "学时应在 1 ~ 500 之间");
                    return;
                }

                if (!isValidSchedule(txtSchedule.getText().trim())) {
                    lblScheduleHint.setText("格式错误，应为 周一 第1-2节");
                    lblScheduleHint.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(dialog, "上课时间格式错误，应为 周一 第1-2节");
                    return;
                }

                int capacity = (int) parseDoubleSafe(txtCapacity.getText().trim(), 50);
                int roomCapacity = (int) parseDoubleSafe(txtRoomCapacity.getText().trim(), 70);
                if (capacity < 1 || capacity > 500) {
                    JOptionPane.showMessageDialog(dialog, "课程容量应在 1 ~ 500 之间");
                    return;
                }
                if (roomCapacity < 1 || roomCapacity > 500) {
                    JOptionPane.showMessageDialog(dialog, "教室容量应在 1 ~ 500 之间");
                    return;
                }
                if (roomCapacity < capacity) {
                    JOptionPane.showMessageDialog(dialog, "教室容量不能小于课程容量");
                    return;
                }

                Course course = new Course();
                course.setCourseId(txtCourseId.getText().trim());
                course.setCourseName(txtCourseName.getText().trim());
                course.setCredit(credit);
                course.setHours(hours);
                course.setCategoryId(txtCategoryId.getText().trim());
                course.setDescription(txtDescription.getText().trim());

                CourseOffering offering = new CourseOffering();
                offering.setOfferingId(txtOfferingId.getText().trim());
                offering.setCourseId(txtCourseId.getText().trim());
                offering.setTeacherId(txtTeacherId.getText().trim());
                offering.setTeacherName(txtTeacherName.getText().trim());
                offering.setSemester(sem);
                offering.setCapacity(capacity);
                offering.setRoomCapacity(roomCapacity);
                offering.setEnrolledCount(0);
                offering.setSchedule(txtSchedule.getText().trim());
                offering.setClassroom(txtClassroom.getText().trim());
                offering.setCampus((String) cbCampus.getSelectedItem());
                offering.setStatus("open");
                offering.setVersion(0);
                offering.setRoundId(currentRound.getRoundId());

                String result = addCourseAndOfferingToServer(course, offering);
                if ("新增成功".equals(result)) {
                    JOptionPane.showMessageDialog(dialog, "新增成功");
                    dialog.dispose();
                    loadCourseList();
                } else {
                    JOptionPane.showMessageDialog(dialog, result, "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "学分、学时、容量必须是数字");
            }
        });
        btnPanel.add(btnOk);

        JButton btnCancel = new JButton("取消");
        btnCancel.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnCancel);

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        l.setForeground(new Color(60, 100, 80));
        return l;
    }

    private JLabel hintLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        l.setForeground(new Color(100, 150, 100));
        return l;
    }

    private Course queryCourseFromServer(String courseId) {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_QUERY_COURSE_BY_ID);
            req.setData(courseId);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) return (Course) resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private Teacher queryTeacherFromServer(String teacherId) {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_QUERY_TEACHER_BY_ID);
            req.setData(teacherId);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) return (Teacher) resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private String addCourseAndOfferingToServer(Course course, CourseOffering offering) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("course", course);
            params.put("offering", offering);
            Message req = new Message();
            req.setType(MsgConst.ADMIN_ADD_COURSE);
            req.setData(params);
            Message resp = SocketClient.send(req);
            return resp.getResponseMsg();
        } catch (Exception e) {
            e.printStackTrace();
            return "连接服务器失败：" + e.getMessage();
        }
    }

    // ============================================================
    // 2. 选课管理
    // ============================================================
    private JPanel createSelectPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(CARD_BG);
        top.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        top.add(new JLabel("搜索学生："));

        txtSearchStudent = new JTextField(18);
        txtSearchStudent.setPreferredSize(new Dimension(200, 28));

        studentPopup = new JPopupMenu();
        studentPopup.setFocusable(false);
        suggestionModel = new DefaultListModel<>();
        studentSuggestionList = new JList<>(suggestionModel);
        studentSuggestionList.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        studentSuggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentSuggestionList.setVisibleRowCount(8);
        JScrollPane popupScroll = new JScrollPane(studentSuggestionList);
        popupScroll.setBorder(null);
        popupScroll.setPreferredSize(new Dimension(300, 180));
        studentPopup.add(popupScroll);
        studentPopup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        suggestionData = new ArrayList<>();

        txtSearchStudent.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onSearchTextChanged(); }
            public void removeUpdate(DocumentEvent e) { onSearchTextChanged(); }
            public void changedUpdate(DocumentEvent e) { onSearchTextChanged(); }
        });

        studentSuggestionList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = studentSuggestionList.getSelectedIndex();
                if (index >= 0 && index < suggestionData.size()) {
                    Map<String, Object> stu = suggestionData.get(index);
                    selectStudent(stu);
                    studentPopup.setVisible(false);
                }
            }
        });

        top.add(txtSearchStudent);

        JButton btnClear = new JButton("清空");
        btnClear.setBackground(BTN_GREEN);
        btnClear.setBorder(new EmptyBorder(5, 16, 5, 16));
        btnClear.addActionListener(e -> {
            txtSearchStudent.setText("");
            availableCoursesModel.setRowCount(0);
            selectedCoursesModel.setRowCount(0);
            lblCurrentStudent.setText("当前学生：未选择");
            currentStudentId = null;
            currentStudentName = null;
            studentPopup.setVisible(false);
        });
        top.add(btnClear);

        lblCurrentStudent = new JLabel("当前学生：未选择");
        lblCurrentStudent.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        lblCurrentStudent.setForeground(new Color(60, 120, 80));
        top.add(lblCurrentStudent);
        panel.add(top, BorderLayout.NORTH);

        JPanel availPanel = createAvailablePanel();
        JPanel selectedPanel = createSelectedPanel();
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, availPanel, selectedPanel);
        mainSplit.setResizeWeight(0.5);
        mainSplit.setDividerSize(8);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        panel.add(mainSplit, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> mainSplit.setDividerLocation(0.5));
        return panel;
    }

    private void onSearchTextChanged() {
        String keyword = txtSearchStudent.getText().trim();
        if (keyword.isEmpty()) { studentPopup.setVisible(false); return; }
        List<Map<String, Object>> students = searchStudentsFromServer(keyword);
        suggestionModel.clear();
        suggestionData.clear();
        if (students == null || students.isEmpty()) { studentPopup.setVisible(false); return; }
        for (Map<String, Object> s : students) {
            String sid = str(s.get("studentId"));
            String sname = str(s.get("studentName"));
            suggestionModel.addElement(sname + " (" + sid + ")");
            suggestionData.add(s);
        }
        if (!studentPopup.isVisible()) studentPopup.show(txtSearchStudent, 0, txtSearchStudent.getHeight());
        else studentPopup.setPopupSize(studentPopup.getPreferredSize());
        studentSuggestionList.setSelectedIndex(0);
    }

    private void selectStudent(Map<String, Object> stu) {
        currentStudentId = str(stu.get("studentId"));
        currentStudentName = str(stu.get("studentName"));
        lblCurrentStudent.setText("当前学生：" + currentStudentName + " (" + currentStudentId + ")");
        txtSearchStudent.setText(currentStudentName + " (" + currentStudentId + ")");
        loadAvailableCoursesForStudent(currentStudentId);
        loadSelectedCoursesForStudent(currentStudentId);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchStudentsFromServer(String keyword) {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_SEARCH_STUDENTS);
            req.setData(keyword);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) return (List<Map<String, Object>>) resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    private JPanel createAvailablePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 5, 5, 5)));
        JLabel title = new JLabel("可选课程");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        title.setForeground(new Color(60, 80, 70));
        title.setBorder(new EmptyBorder(3, 5, 5, 5));
        p.add(title, BorderLayout.NORTH);

        String[] cols = {"开课ID", "课程号", "课程名", "教师", "上课时间", "已选/容量", "操作"};
        availableCoursesModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(availableCoursesModel);
        table.setRowHeight(28);
        table.getTableHeader().setBackground(HEADER_BG);
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        TableColumn c0 = table.getColumnModel().getColumn(0);
        c0.setMinWidth(0); c0.setMaxWidth(0); c0.setPreferredWidth(0);
        table.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer("选课"));
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 6 && currentStudentId != null) {
                    Object id = table.getValueAt(row, 0);
                    if (id != null) doAdminSelect(id.toString());
                }
            }
        });
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel createSelectedPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 5, 5, 5)));
        JLabel title = new JLabel("已选课程");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        title.setForeground(new Color(60, 80, 70));
        title.setBorder(new EmptyBorder(3, 5, 5, 5));
        p.add(title, BorderLayout.NORTH);

        String[] cols = {"开课ID", "课程号", "课程名", "教师", "上课时间", "操作"};
        selectedCoursesModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(selectedCoursesModel);
        table.setRowHeight(28);
        table.getTableHeader().setBackground(HEADER_BG);
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        TableColumn c0 = table.getColumnModel().getColumn(0);
        c0.setMinWidth(0); c0.setMaxWidth(0); c0.setPreferredWidth(0);
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer("退课"));
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 5 && currentStudentId != null) {
                    Object id = table.getValueAt(row, 0);
                    if (id != null) doAdminDrop(id.toString());
                }
            }
        });
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    @SuppressWarnings("unchecked")
    private void loadAvailableCoursesForStudent(String studentId) {
        availableCoursesModel.setRowCount(0);
        try {
            Map<String, String> params = new HashMap<>();
            params.put("studentId", studentId);
            params.put("roundId", currentRound != null ? currentRound.getRoundId() : null);
            Message req = new Message();
            req.setType(MsgConst.ADMIN_GET_AVAILABLE_FOR_STUDENT);
            req.setData(params);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) resp.getData();
                for (Map<String, Object> c : list) {
                    availableCoursesModel.addRow(new Object[]{
                            c.get("offeringId"), c.get("courseId"), c.get("courseName"),
                            c.get("teacherName"), c.get("schedule"),
                            c.get("enrolledCount") + "/" + c.get("capacity"), "选课"
                    });
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    private void loadSelectedCoursesForStudent(String studentId) {
        selectedCoursesModel.setRowCount(0);
        try {
            Map<String, String> params = new HashMap<>();
            params.put("studentId", studentId);
            params.put("roundId", currentRound != null ? currentRound.getRoundId() : null);
            Message req = new Message();
            req.setType(MsgConst.ADMIN_GET_STUDENT_STATUS);
            req.setData(params);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) resp.getData();
                for (Map<String, Object> c : list) {
                    selectedCoursesModel.addRow(new Object[]{
                            c.get("offeringId"), c.get("courseId"), c.get("courseName"),
                            c.get("teacherName"), c.get("schedule"), "退课"
                    });
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void doAdminSelect(String offeringId) {
        if (currentStudentId == null) {
            JOptionPane.showMessageDialog(this, "请先选择学生");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "确认为学生 " + currentStudentName + "(" + currentStudentId + ") 选这门课？\n"
                        + "（管理员操作无视时间冲突和关闭状态，但受教室容量上限约束）",
                "确认选课", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        final String sid = currentStudentId;

        new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() {
                Map<String, String> params = new HashMap<>();
                params.put("studentId", sid);
                params.put("offeringId", offeringId);
                Message req = new Message();
                req.setType(MsgConst.ADMIN_HELP_SELECT);
                req.setData(params);
                return SocketClient.send(req);
            }

            @Override
            protected void done() {
                try {
                    Message resp = get();
                    if (resp != null && resp.isSuccess()) {
                        JOptionPane.showMessageDialog(AdminCoursePanel.this, "选课成功");
                        loadAvailableCoursesForStudent(sid);
                        loadSelectedCoursesForStudent(sid);
                        loadCourseList();
                    } else {
                        JOptionPane.showMessageDialog(AdminCoursePanel.this,
                                "选课失败: " + (resp != null ? resp.getResponseMsg() : "无响应"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(AdminCoursePanel.this,
                            "系统异常: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void doAdminDrop(String offeringId) {
        if (currentStudentId == null) {
            JOptionPane.showMessageDialog(this, "请先选择学生");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "确认为学生 " + currentStudentName + "(" + currentStudentId + ") 退选这门课？",
                "确认退课", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        final String sid = currentStudentId;

        new SwingWorker<Message, Void>() {
            @Override
            protected Message doInBackground() {
                Map<String, String> params = new HashMap<>();
                params.put("studentId", sid);
                params.put("offeringId", offeringId);
                Message req = new Message();
                req.setType(MsgConst.ADMIN_HELP_DROP);
                req.setData(params);
                return SocketClient.send(req);
            }

            @Override
            protected void done() {
                try {
                    Message resp = get();
                    if (resp != null && resp.isSuccess()) {
                        JOptionPane.showMessageDialog(AdminCoursePanel.this, "退课成功");
                        loadAvailableCoursesForStudent(sid);
                        loadSelectedCoursesForStudent(sid);
                        loadCourseList();
                    } else {
                        JOptionPane.showMessageDialog(AdminCoursePanel.this,
                                "退课失败: " + (resp != null ? resp.getResponseMsg() : "无响应"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(AdminCoursePanel.this,
                            "系统异常: " + e.getMessage());
                }
            }
        }.execute();
    }

    // ============================================================
    // 3. 选课轮次
    // ============================================================
    private JPanel createRoundPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topBar.setBackground(CARD_BG);
        topBar.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        JLabel title = new JLabel("历史选课轮次");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        title.setForeground(new Color(40, 60, 50));
        topBar.add(title);

        JLabel hint = new JLabel("（点击卡片切换当前轮次）");
        hint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        hint.setForeground(new Color(120, 140, 130));
        topBar.add(hint);

        JButton btnAdd = new JButton("+ 新增选课轮次");
        btnAdd.setBackground(BTN_GREEN);
        btnAdd.setBorder(new EmptyBorder(5, 20, 5, 20));
        btnAdd.addActionListener(e -> showAddRoundDialog());
        topBar.add(btnAdd);
        panel.add(topBar, BorderLayout.NORTH);
        JButton btnClone = new JButton("复制轮次");
        btnClone.setBackground(new Color(200, 220, 240));
        btnClone.setBorder(new EmptyBorder(5, 20, 5, 20));
        btnClone.addActionListener(e -> showCloneRoundDialog());
        topBar.add(btnClone);
        roundsPanel = new JPanel();
        roundsPanel.setLayout(new BoxLayout(roundsPanel, BoxLayout.Y_AXIS));
        roundsPanel.setBackground(BG_COLOR);
        JScrollPane sp = new JScrollPane(roundsPanel);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setBackground(BG_COLOR);
        sp.getViewport().setBackground(BG_COLOR);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private void loadRounds() {
        roundsPanel.removeAll();
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_GET_ALL_ROUNDS);
            req.setData(null);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                List<SelectRound> rounds = (List<SelectRound>) resp.getData();
                if (rounds.isEmpty()) {
                    JLabel empty = new JLabel("暂无选课轮次", SwingConstants.CENTER);
                    empty.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
                    empty.setForeground(new Color(150, 150, 150));
                    empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                    roundsPanel.add(Box.createVerticalStrut(50));
                    roundsPanel.add(empty);
                } else {
                    for (SelectRound r : rounds) {
                        roundsPanel.add(createRoundCard(r));
                        roundsPanel.add(Box.createVerticalStrut(10));
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        roundsPanel.revalidate();
        roundsPanel.repaint();
    }

    private JPanel createRoundCard(SelectRound round) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        card.setPreferredSize(new Dimension(750, 130));

        boolean isCurrent = currentRound != null && currentRound.getRoundId().equals(round.getRoundId());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isCurrent ? new Color(100, 180, 120) : BORDER_COLOR, isCurrent ? 2 : 1),
                new EmptyBorder(isCurrent ? 13 : 14, isCurrent ? 18 : 19, isCurrent ? 13 : 14, isCurrent ? 18 : 19)));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(CARD_BG);

        JLabel name = new JLabel(round.getRoundName() + (isCurrent ? "  [当前]" : ""));
        name.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        name.setForeground(isCurrent ? new Color(60, 150, 80) : new Color(40, 60, 50));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(name);
        left.add(Box.createVerticalStrut(8));

        JLabel sem = new JLabel("学期：" + round.getSemester());
        sem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        sem.setForeground(new Color(100, 100, 100));
        sem.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(sem);
        left.add(Box.createVerticalStrut(4));

        JLabel time = new JLabel("开始：" + round.getStartTime() + "    结束：" + round.getEndTime());
        time.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        time.setForeground(new Color(100, 100, 100));
        time.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(time);
        left.add(Box.createVerticalStrut(8));

        String statusText;
        Color statusColor;
        if ("open".equals(round.getStatus())) { statusText = "开放中"; statusColor = new Color(60, 150, 80); }
        else if ("closed".equals(round.getStatus())) { statusText = "已结束"; statusColor = new Color(180, 80, 80); }
        else { statusText = "未开始"; statusColor = new Color(200, 150, 50); }
        JLabel status = new JLabel(statusText);
        status.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        status.setForeground(statusColor);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(status);
        card.add(left, BorderLayout.CENTER);

        JPanel btns = new JPanel();
        btns.setLayout(new BoxLayout(btns, BoxLayout.Y_AXIS));
        btns.setBackground(CARD_BG);

        if ("open".equals(round.getStatus())) {
            JButton btnClose = new JButton("关闭选课");
            btnClose.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            btnClose.setBackground(BTN_DANGER);
            btnClose.setBorder(new EmptyBorder(6, 20, 6, 20));
            btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnClose.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnClose.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "确定关闭轮次 \"" + round.getRoundName() + "\" 吗？\n该轮次下所有课程的状态会变为 closed。",
                        "确认关闭", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION && closeRound(round.getRoundId())) {
                    if (currentRound != null && currentRound.getRoundId().equals(round.getRoundId())) currentRound = null;
                    JOptionPane.showMessageDialog(this, "已关闭该轮次");
                    loadRounds();
                    updateHeaderRound();
                    loadCourseList();
                }
            });
            btns.add(btnClose);
        } else if ("closed".equals(round.getStatus())) {
            JButton btnOpen = new JButton("重新开放");
            btnOpen.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
            btnOpen.setBackground(BTN_GREEN);
            btnOpen.setBorder(new EmptyBorder(6, 20, 6, 20));
            btnOpen.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnOpen.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnOpen.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "确定重新开放轮次 \"" + round.getRoundName() + "\" 吗？\n其他开放的轮次会被自动关闭",
                        "确认开放", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION && openRound(round.getRoundId())) {
                    JOptionPane.showMessageDialog(this, "已开放该轮次");
                    loadRounds();
                    updateHeaderRound();
                    loadCourseList();
                }
            });
            btns.add(btnOpen);
        }
        btns.add(Box.createVerticalStrut(5));
        JButton btnEdit = new JButton("修改");
        btnEdit.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        btnEdit.setBackground(new Color(220, 230, 245));
        btnEdit.setForeground(new Color(40, 60, 100));
        btnEdit.setBorder(new EmptyBorder(4, 15, 4, 15));
        btnEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEdit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEdit.addActionListener(e -> {
            showEditRoundDialog(round);
        });
        btns.add(btnEdit);
        btns.add(Box.createVerticalStrut(5));
        JButton btnDelete = new JButton("删除");
        btnDelete.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        btnDelete.setBackground(new Color(230, 230, 230));
        btnDelete.setForeground(new Color(150, 50, 50));
        btnDelete.setBorder(new EmptyBorder(4, 15, 4, 15));
        btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDelete.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "确定删除轮次 \"" + round.getRoundName() + "\" 吗？",
                    "确认删除", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION && deleteRound(round.getRoundId())) {
                if (currentRound != null && currentRound.getRoundId().equals(round.getRoundId())) currentRound = null;
                JOptionPane.showMessageDialog(this, "已删除");
                loadRounds();
                updateHeaderRound();
                loadCourseList();
            }
        });
        btns.add(btnDelete);
        card.add(btns, BorderLayout.EAST);

        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        MouseAdapter switchListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() instanceof JButton) return;
                Object src = e.getSource();
                if (src instanceof Component) {
                    Component c = (Component) src;
                    if (SwingUtilities.getAncestorOfClass(JButton.class, c) != null) return;
                }
                currentRound = round;
                updateHeaderRound();
                loadCourseList();
                availableCoursesModel.setRowCount(0);
                selectedCoursesModel.setRowCount(0);
                lblCurrentStudent.setText("当前学生：未选择");
                currentStudentId = null;
                currentStudentName = null;
                txtSearchStudent.setText("");
                loadRounds();
            }
        };
        attachClickListener(card, switchListener);
        return card;
    }

    private void attachClickListener(Container container, MouseAdapter listener) {
        container.setCursor(new Cursor(Cursor.HAND_CURSOR));
        container.addMouseListener(listener);
        for (Component c : container.getComponents()) {
            if (c instanceof JButton) continue;
            if (c instanceof Container) attachClickListener((Container) c, listener);
            else {
                c.setCursor(new Cursor(Cursor.HAND_CURSOR));
                c.addMouseListener(listener);
            }
        }
    }

    private void showAddRoundDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "新增选课轮次", true);
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtRoundId = new JTextField(18);
        JTextField txtSemester = new JTextField(18);
        JTextField txtRoundName = new JTextField(18);

        JTextField txtStartTime = new JTextField(18);
        JLabel lblStartHint = new JLabel("格式：yyyy-MM-dd HH:mm:ss（例：2025-09-11 19:04:00）");
        lblStartHint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        lblStartHint.setForeground(new Color(120, 120, 120));

        JTextField txtEndTime = new JTextField(18);
        JLabel lblEndHint = new JLabel("格式：yyyy-MM-dd HH:mm:ss（例：2025-09-20 23:59:00）");
        lblEndHint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        lblEndHint.setForeground(new Color(120, 120, 120));

        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"upcoming", "open", "closed"});

        txtStartTime.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String s = txtStartTime.getText().trim();
                if (s.isEmpty()) {
                    lblStartHint.setText("格式：yyyy-MM-dd HH:mm:ss（例：2025-09-11 19:04:00）");
                    lblStartHint.setForeground(new Color(120, 120, 120));
                    return;
                }
                String norm = normalizeDateTime(s);
                if (norm == null) {
                    lblStartHint.setText("格式错误，应为 yyyy-MM-dd HH:mm:ss");
                    lblStartHint.setForeground(Color.RED);
                } else {
                    lblStartHint.setText("已识别：" + norm);
                    lblStartHint.setForeground(new Color(60, 150, 80));
                    txtStartTime.setText(norm);
                }
            }
        });

        txtEndTime.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String s = txtEndTime.getText().trim();
                if (s.isEmpty()) {
                    lblEndHint.setText("格式：yyyy-MM-dd HH:mm:ss（例：2025-09-20 23:59:00）");
                    lblEndHint.setForeground(new Color(120, 120, 120));
                    return;
                }
                String norm = normalizeDateTime(s);
                if (norm == null) {
                    lblEndHint.setText("格式错误，应为 yyyy-MM-dd HH:mm:ss");
                    lblEndHint.setForeground(Color.RED);
                } else {
                    lblEndHint.setText("已识别：" + norm);
                    lblEndHint.setForeground(new Color(60, 150, 80));
                    txtEndTime.setText(norm);
                }
            }
        });

        txtSemester.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String sem = txtSemester.getText().trim();
                if (!sem.isEmpty()) {
                    if (txtRoundName.getText().trim().isEmpty()) txtRoundName.setText(sem + "选课记录");
                    if (txtRoundId.getText().trim().isEmpty()) txtRoundId.setText("ROUND-" + sem);
                }
            }
        });

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("轮次ID："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtRoundId, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("学期："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtSemester, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("轮次名称："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtRoundName, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("开始时间："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtStartTime, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1; form.add(lblStartHint, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("结束时间："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtEndTime, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1; form.add(lblEndHint, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("状态："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(cbStatus, gbc);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(Color.WHITE);

        JButton btnOk = new JButton("确定");
        btnOk.setBackground(BTN_GREEN);
        btnOk.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnOk.addActionListener(e -> {
            if (txtRoundId.getText().trim().isEmpty() || txtSemester.getText().trim().isEmpty()
                    || txtRoundName.getText().trim().isEmpty()
                    || txtStartTime.getText().trim().isEmpty()
                    || txtEndTime.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "请填写完整信息");
                return;
            }

            String startNorm = normalizeDateTime(txtStartTime.getText().trim());
            String endNorm = normalizeDateTime(txtEndTime.getText().trim());

            if (startNorm == null) {
                lblStartHint.setText("格式错误，应为 yyyy-MM-dd HH:mm:ss");
                lblStartHint.setForeground(Color.RED);
                JOptionPane.showMessageDialog(dialog, "开始时间格式错误，请检查");
                return;
            }
            if (endNorm == null) {
                lblEndHint.setText("格式错误，应为 yyyy-MM-dd HH:mm:ss");
                lblEndHint.setForeground(Color.RED);
                JOptionPane.showMessageDialog(dialog, "结束时间格式错误，请检查");
                return;
            }

            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.util.Date startDate = sdf.parse(startNorm);
                java.util.Date endDate = sdf.parse(endNorm);

                if (!startDate.before(endDate)) {
                    JOptionPane.showMessageDialog(dialog, "开始时间必须早于结束时间");
                    return;
                }
                if (endDate.before(new java.util.Date())) {
                    JOptionPane.showMessageDialog(dialog, "结束时间不能早于当前时间");
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "时间解析失败：" + ex.getMessage());
                return;
            }

            SelectRound r = new SelectRound();
            r.setRoundId(txtRoundId.getText().trim());
            r.setSemester(txtSemester.getText().trim());
            r.setRoundName(txtRoundName.getText().trim());
            r.setStartTime(startNorm);
            r.setEndTime(endNorm);
            r.setStatus((String) cbStatus.getSelectedItem());

            if (addRound(r)) {
                JOptionPane.showMessageDialog(dialog, "新增成功");
                dialog.dispose();
                loadRounds();
                updateHeaderRound();
            } else {
                JOptionPane.showMessageDialog(dialog, "新增失败，请检查轮次ID是否已存在");
            }
        });
        btnPanel.add(btnOk);

        JButton btnCancel = new JButton("取消");
        btnCancel.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnCancel);

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private boolean addRound(SelectRound round) {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_ADD_ROUND);
            req.setData(round);
            Message resp = SocketClient.send(req);
            return resp != null && resp.isSuccess();
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private boolean closeRound(String roundId) {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_CLOSE_ROUND);
            req.setData(roundId);
            Message resp = SocketClient.send(req);
            return resp != null && resp.isSuccess();
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private boolean openRound(String roundId) {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_OPEN_ROUND);
            req.setData(roundId);
            Message resp = SocketClient.send(req);
            return resp != null && resp.isSuccess();
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private boolean deleteRound(String roundId) {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_DELETE_ROUND);
            req.setData(roundId);
            Message resp = SocketClient.send(req);
            return resp != null && resp.isSuccess();
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private void updateHeaderRound() {
        if (lblCurrentRoundHeader == null) return;
        if (currentRound == null) currentRound = fetchCurrentOpenRound();

        if (currentRound != null) {
            String statusText = "open".equals(currentRound.getStatus()) ? "开放中" :
                    "closed".equals(currentRound.getStatus()) ? "已结束" : "未开始";
            lblCurrentRoundHeader.setText("当前轮次：" + currentRound.getRoundName() + " [" + statusText + "]");
            lblCurrentRoundHeader.setForeground("open".equals(currentRound.getStatus())
                    ? new Color(60, 150, 80) : new Color(180, 80, 80));
        } else {
            lblCurrentRoundHeader.setText("当前无开放轮次");
            lblCurrentRoundHeader.setForeground(new Color(180, 80, 80));
        }
        scheduleNextRoundRefresh();
    }

    private SelectRound fetchCurrentOpenRound() {
        try {
            Message req = new Message();
            req.setType(MsgConst.GET_CURRENT_ROUND);
            req.setData(null);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) return (SelectRound) resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // ============================================================
    // 批量新增课程对话框
    // ============================================================
    private void showBatchAddCourseDialog() {
        if (currentRound == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个选课轮次", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Program> programs = fetchAllPrograms();
        if (programs == null || programs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可用的培养方案", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "批量新增课程", true);
        dialog.setSize(750, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("培养方案："), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JComboBox<String> cbProgram = new JComboBox<>();
        Map<String, String> programIdMap = new LinkedHashMap<>();
        for (Program p : programs) {
            String display = p.getProgramName() + " (" + p.getMajor() + " / " + p.getGrade() + ")";
            cbProgram.addItem(display);
            programIdMap.put(display, p.getProgramId());
        }
        topPanel.add(cbProgram, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        topPanel.add(new JLabel("学期："), gbc);
        gbc.gridx = 1;
        JTextField txtSemester = new JTextField(currentRound.getSemester(), 18);
        topPanel.add(txtSemester, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton btnQuery = new JButton("查询课程");
        btnQuery.setBackground(BTN_BLUE);
        btnQuery.setBorder(new EmptyBorder(6, 25, 6, 25));
        topPanel.add(btnQuery, gbc);
        dialog.add(topPanel, BorderLayout.NORTH);

        String[] cols = {"课程号", "课程名", "学分", "课程性质", "教学班数量"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.getTableHeader().setBackground(HEADER_BG);
        table.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(new Color(240, 250, 240));
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BG_COLOR);
        centerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        centerPanel.add(sp, BorderLayout.CENTER);
        dialog.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(10, 20, 15, 20));

        JLabel lblPreview = new JLabel("匹配课程数：—");
        lblPreview.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        lblPreview.setForeground(new Color(60, 100, 80));
        bottomPanel.add(lblPreview, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(Color.WHITE);

        JButton btnOk = new JButton("确认生成");
        btnOk.setBackground(BTN_GREEN);
        btnOk.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnOk.setEnabled(false);

        JButton btnCancel = new JButton("取消");
        btnCancel.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnCancel.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnOk);
        btnPanel.add(btnCancel);
        bottomPanel.add(btnPanel, BorderLayout.EAST);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        btnQuery.addActionListener(e -> {
            String pid = programIdMap.get((String) cbProgram.getSelectedItem());
            String sem = txtSemester.getText().trim();
            if (pid == null || sem.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "请选择培养方案并填写学期");
                return;
            }
            List<ProgramCourse> courses = fetchProgramCourses(pid);
            List<ProgramCourse> matched = new ArrayList<>();
            for (ProgramCourse pc : courses) {
                if (sem.equals(pc.getRecommendSemester())) matched.add(pc);
            }
            model.setRowCount(0);
            if (matched.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "该培养方案在该学期下没有推荐课程");
                lblPreview.setText("匹配课程数：0");
                btnOk.setEnabled(false);
                return;
            }
            matched.sort(Comparator.comparing(ProgramCourse::getCourseId));

            List<String> courseIds = new ArrayList<>();
            for (ProgramCourse pc : matched) {
                courseIds.add(pc.getCourseId());
            }
            Map<String, String> courseNameMap = fetchCourseNamesByIds(courseIds);

            for (ProgramCourse pc : matched) {
                String courseName = courseNameMap.getOrDefault(pc.getCourseId(), pc.getCourseId());
                model.addRow(new Object[]{
                        pc.getCourseId(),
                        courseName,
                        pc.getCreditWeight(),
                        pc.getCourseType() != null ? pc.getCourseType() : "",
                        1
                });
            }
            lblPreview.setText("匹配课程数：" + matched.size() + " 门");
            btnOk.setEnabled(true);
        });

        btnOk.addActionListener(e -> {
        	if (table.isEditing()) table.getCellEditor().stopCellEditing();
            Map<String, Integer> classCountMap = new LinkedHashMap<>();
            int totalClasses = 0;
            int skipCount = 0;
            for (int i = 0; i < model.getRowCount(); i++) {
                String courseId = String.valueOf(model.getValueAt(i, 0));
                Object cntObj = model.getValueAt(i, 4);
                int cnt;
                try {
                    cnt = Integer.parseInt(String.valueOf(cntObj).trim());
                } catch (NumberFormatException ex) {
                    cnt = 1;
                }
                if (cnt <= 0) {
                    skipCount++;
                    continue;
                }
                if (cnt > 20) cnt = 20;
                classCountMap.put(courseId, cnt);
                totalClasses += cnt;
            }

            if (classCountMap.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "所有课程的教学班数量都填了 0，没有任何课程需要生成。",
                        "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "将为 " + classCountMap.size() + " 门课，共生成 " + totalClasses + " 条开课记录。"
                            + "\n确定继续吗？",
                    "确认生成", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            String pid = programIdMap.get((String) cbProgram.getSelectedItem());
            String sem = txtSemester.getText().trim();

            try {
                Map<String, Object> params = new HashMap<>();
                params.put("programId", pid);
                params.put("semester", sem);
                params.put("roundId", currentRound != null ? currentRound.getRoundId() : null);
                params.put("classCountMap", classCountMap);

                Message req = new Message();
                req.setType(MsgConst.ADMIN_BATCH_ADD_COURSE);
                req.setData(params);

                Message resp = SocketClient.send(req);
                if (resp != null && resp.isSuccess() && resp.getData() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) resp.getData();
                    StringBuilder msg = new StringBuilder();
                    msg.append("批量生成完成！\n");
                    msg.append("涉及课程：").append(result.get("totalCount")).append(" 门\n");
                    msg.append("跳过课程：").append(result.get("skipCount")).append(" 门\n");
                    msg.append("新建记录：").append(result.get("successCount")).append(" 条\n");
                    Object errs = result.get("errors");
                    if (errs instanceof List && !((List<?>) errs).isEmpty()) {
                        msg.append("\n失败明细：\n");
                        for (Object o : (List<?>) errs) msg.append("  ").append(o).append("\n");
                    }
                    msg.append("\n请到课程列表，逐条点“修改”填写教师、教室、上课时间。");
                    JOptionPane.showMessageDialog(dialog, msg.toString(), "生成结果", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadCourseList();
                } else {
                    JOptionPane.showMessageDialog(dialog, resp != null ? resp.getResponseMsg() : "未知错误", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "生成失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fetchCourseNamesByIds(List<String> courseIds) {
        Map<String, String> result = new HashMap<>();
        if (courseIds == null || courseIds.isEmpty()) return result;
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_GET_COURSES_BY_IDS);
            req.setData(courseIds);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                List<Course> courses = (List<Course>) resp.getData();
                for (Course c : courses) {
                    result.put(c.getCourseId(), c.getCourseName() != null ? c.getCourseName() : c.getCourseId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // ============================================================
    // 辅助方法
    // ============================================================
    @SuppressWarnings("unchecked")
    private List<Program> fetchAllPrograms() {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_GET_ALL_PROGRAMS);
            req.setData(null);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) return (List<Program>) resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<ProgramCourse> fetchProgramCourses(String programId) {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_GET_PROGRAM_COURSES);
            req.setData(programId);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) return (List<ProgramCourse>) resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    private String generateOfferingId(String courseId, String semester) {
        List<String> existing = fetchOfferingIdsByCourseAndSemester(courseId, semester);
        for (int seq = 1; seq <= 99; seq++) {
            String candidate = courseId + "-" + semester + "-" + String.format("%02d", seq);
            if (!existing.contains(candidate)) return candidate;
        }
        return courseId + "-" + semester + "-" + System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    private List<String> fetchOfferingIdsByCourseAndSemester(String courseId, String semester) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("courseId", courseId);
            params.put("semester", semester);
            Message req = new Message();
            req.setType(MsgConst.ADMIN_GET_OFFERING_IDS);
            req.setData(params);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) return (List<String>) resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    private String normalizeDateTime(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return null;
        s = s.replace("：", ":").replace("。", ".").replace("/", "-").replace(".", "-");
        String[] patterns = {
                "yyyy-M-d HH:mm:ss", "yyyy-M-d HH:mm", "yyyy-M-d",
                "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"
        };
        for (String p : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(p);
                sdf.setLenient(false);
                java.util.Date d = sdf.parse(s);
                java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return out.format(d);
            } catch (Exception ignore) { }
        }
        return null;
    }

    private void scheduleNextRoundRefresh() {
        if (roundTimer != null) { roundTimer.stop(); roundTimer = null; }

        long delayMillis = 10_000;

        List<SelectRound> allRounds = fetchAllRounds();
        if (allRounds != null && !allRounds.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                long now = System.currentTimeMillis();
                long next = -1;

                for (SelectRound r : allRounds) {
                    try {
                        long start = sdf.parse(r.getStartTime()).getTime();
                        long end = sdf.parse(r.getEndTime()).getTime();
                        if (start > now && (next == -1 || start < next)) next = start;
                        if (end > now && (next == -1 || end < next)) next = end;
                    } catch (Exception ignore) { }
                }

                if (next > 0) {
                    delayMillis = Math.max(0, next - now) + 500;
                    System.out.println("[AdminCoursePanel] 下次自动刷新在 " + delayMillis + " 毫秒后");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (delayMillis > Integer.MAX_VALUE) delayMillis = 60 * 60 * 1000;

        final long d = delayMillis;
        roundTimer = new javax.swing.Timer((int) Math.min(d, Integer.MAX_VALUE), e -> {
            if (roundTimer != null) { roundTimer.stop(); roundTimer = null; }
            System.out.println("[AdminCoursePanel] 到点，自动刷新");

            if (currentRound == null) {
                updateHeaderRound();
            } else {
                refreshCurrentRoundStatus();
            }

            loadRoundsKeepSelection();

            loadCourseList();

            scheduleNextRoundRefresh();
        });
        roundTimer.setRepeats(false);
        roundTimer.start();
    }

    private void refreshCurrentRoundStatus() {
        if (currentRound == null) return;
        List<SelectRound> allRounds = fetchAllRounds();
        if (allRounds == null) return;
        for (SelectRound r : allRounds) {
            if (r.getRoundId().equals(currentRound.getRoundId())) {
                currentRound = r;
                break;
            }
        }
        if (lblCurrentRoundHeader != null) {
            String statusText = "open".equals(currentRound.getStatus()) ? "开放中" :
                    "closed".equals(currentRound.getStatus()) ? "已结束" : "未开始";
            lblCurrentRoundHeader.setText("当前轮次：" + currentRound.getRoundName() + " [" + statusText + "]");
            lblCurrentRoundHeader.setForeground("open".equals(currentRound.getStatus())
                    ? new Color(60, 150, 80) : new Color(180, 80, 80));
        }
    }

    private void loadRoundsKeepSelection() {
        roundsPanel.removeAll();
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_GET_ALL_ROUNDS);
            req.setData(null);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                @SuppressWarnings("unchecked")
                List<SelectRound> rounds = (List<SelectRound>) resp.getData();
                if (rounds.isEmpty()) {
                    JLabel empty = new JLabel("暂无选课轮次", SwingConstants.CENTER);
                    empty.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
                    empty.setForeground(new Color(150, 150, 150));
                    empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                    roundsPanel.add(Box.createVerticalStrut(50));
                    roundsPanel.add(empty);
                } else {
                    for (SelectRound r : rounds) {
                        roundsPanel.add(createRoundCard(r));
                        roundsPanel.add(Box.createVerticalStrut(10));
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        roundsPanel.revalidate();
        roundsPanel.repaint();
    }

    @Override
  
    public void removeNotify() {
        if (roundTimer != null) { roundTimer.stop(); roundTimer = null; }
        if (pollTimer != null) { pollTimer.stop(); pollTimer = null; }
        super.removeNotify();
    }

    private void exportOfferingTemplate() {
        if (currentRound == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个选课轮次", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("roundId", currentRound.getRoundId());
            Message req = new Message();
            req.setType(MsgConst.ADMIN_EXPORT_OFFERING_TEMPLATE);
            req.setData(params);
            Message resp = SocketClient.send(req);
            if (resp == null || !resp.isSuccess() || resp.getData() == null) {
                JOptionPane.showMessageDialog(this, resp != null ? resp.getResponseMsg() : "未知错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            byte[] bytes = (byte[]) resp.getData();
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("课表模板_" + currentRound.getSemester() + ".xlsx"));
            fc.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File file = fc.getSelectedFile();
            if (!file.getName().endsWith(".xlsx")) file = new File(file.getAbsolutePath() + ".xlsx");
            try (FileOutputStream fos = new FileOutputStream(file)) { fos.write(bytes); }
            JOptionPane.showMessageDialog(this, "导出成功：\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importOfferingExcel() {
        if (currentRound == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个选课轮次", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String tips = "Excel 格式要求：\n" +
                "第一行：表头\n" +
                "列：offeringId | teacherId | teacherName | schedule | classroom | campus | capacity | roomCapacity\n\n" +
                "说明：\n" +
                "- offeringId 必须是系统里已有的\n" +
                "- schedule 格式：周一 第1-2节\n" +
                "- roomCapacity 留空则保持原值\n\n是否继续？";
        if (JOptionPane.showConfirmDialog(this, tips, "导入说明", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx, *.xls)", "xlsx", "xls"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        byte[] fileBytes;
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) > 0) baos.write(buf, 0, n);
            fileBytes = baos.toByteArray();
        } catch (Exception ioe) {
            JOptionPane.showMessageDialog(this, "读取文件失败：" + ioe.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("fileName", file.getName());
            params.put("fileBytes", fileBytes);
            Message req = new Message();
            req.setType(MsgConst.ADMIN_IMPORT_OFFERING_EXCEL);
            req.setData(params);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) resp.getData();
                StringBuilder msg = new StringBuilder();
                msg.append("导入完成！\n");
                msg.append("总行数：").append(result.get("totalRows")).append("\n");
                msg.append("成功：").append(result.get("successCount")).append(" 条\n");
                msg.append("未找到：").append(result.get("notFoundCount")).append(" 条\n");
                Object errs = result.get("errors");
                if (errs instanceof List && !((List<?>) errs).isEmpty()) {
                    msg.append("\n失败明细（前10条）：\n");
                    int cnt = 0;
                    for (Object o : (List<?>) errs) {
                        if (cnt++ >= 10) { msg.append("...（还有更多）\n"); break; }
                        msg.append("  ").append(o).append("\n");
                    }
                }
                JOptionPane.showMessageDialog(this, msg.toString(), "导入结果", JOptionPane.INFORMATION_MESSAGE);
                loadCourseList();
            } else {
                JOptionPane.showMessageDialog(this, resp != null ? resp.getResponseMsg() : "未知错误", "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "导入失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String str(Object obj) { return obj == null ? "" : obj.toString(); }

    private double parseDoubleSafe(String s, double def) {
        try {
            if (s == null || s.trim().isEmpty()) return def;
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) { return def; }
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {
        private String defaultText;

        public ButtonRenderer(String defaultText) {
            this.defaultText = defaultText;
            setOpaque(true);
            setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText(value != null ? value.toString() : defaultText);
            if ("课程详情".equals(getText()) || "保存".equals(getText())) setBackground(BTN_BLUE);
            else if ("退课".equals(getText()) || "删除".equals(getText())) setBackground(new Color(240, 180, 180));
            else if ("修改".equals(getText())) setBackground(new Color(240, 220, 150));
            else setBackground(BTN_GREEN);
            if (isSelected) setBackground(new Color(160, 200, 170));
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    private void showCloneRoundDialog() {
        List<SelectRound> rounds = fetchAllRounds();
        if (rounds == null || rounds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "暂无选课轮次", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "复制轮次开课", true);
        dialog.setSize(480, 340);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Map<String, SelectRound> roundMap = new LinkedHashMap<>();
        List<String> items = new ArrayList<>();
        for (SelectRound r : rounds) {
            String display = r.getRoundName() + " (" + r.getSemester() + ")";
            items.add(display);
            roundMap.put(display, r);
        }

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("源轮次："), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JComboBox<String> cbSource = new JComboBox<>(items.toArray(new String[0]));
        form.add(cbSource, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("目标轮次："), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JComboBox<String> cbTarget = new JComboBox<>(items.toArray(new String[0]));
        if (items.size() > 1) cbTarget.setSelectedIndex(1);
        form.add(cbTarget, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel lblHint = new JLabel("<html>说明：<br>" +
                "- 将源轮次的所有开课记录复制到目标轮次<br>" +
                "- offeringId 会重新生成<br>" +
                "- 已选人数归 0，状态默认 closed<br>" +
                "- 教师/时间/教室/容量等信息保留</html>");
        lblHint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        lblHint.setForeground(new Color(120, 120, 120));
        form.add(lblHint, gbc);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(Color.WHITE);

        JButton btnOk = new JButton("开始复制");
        btnOk.setBackground(BTN_GREEN);
        btnOk.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnOk.addActionListener(e -> {
            SelectRound src = roundMap.get((String) cbSource.getSelectedItem());
            SelectRound tgt = roundMap.get((String) cbTarget.getSelectedItem());
            if (src == null || tgt == null) return;
            if (src.getRoundId().equals(tgt.getRoundId())) {
                JOptionPane.showMessageDialog(dialog, "源轮次和目标轮次不能相同");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "将把「" + src.getRoundName() + "」的所有开课记录复制到「" + tgt.getRoundName() + "」。\n" +
                    "offeringId 会重新生成，已选人数归 0。\n\n确定继续吗？",
                    "确认复制", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            try {
                Map<String, Object> params = new HashMap<>();
                params.put("sourceRoundId", src.getRoundId());
                params.put("targetRoundId", tgt.getRoundId());

                Message req = new Message();
                req.setType(MsgConst.ADMIN_CLONE_ROUND_OFFERINGS);
                req.setData(params);
                Message resp = SocketClient.send(req);
                if (resp != null && resp.isSuccess() && resp.getData() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) resp.getData();
                    StringBuilder msg = new StringBuilder();
                    msg.append("复制完成！\n");
                    msg.append("源轮次开课：").append(result.get("totalCount")).append(" 条\n");
                    msg.append("成功复制：").append(result.get("successCount")).append(" 条\n");
                    Object errs = result.get("errors");
                    if (errs instanceof List && !((List<?>) errs).isEmpty()) {
                        msg.append("\n失败明细（前10条）：\n");
                        int cnt = 0;
                        for (Object o : (List<?>) errs) {
                            if (cnt++ >= 10) { msg.append("...（还有更多）\n"); break; }
                            msg.append("  ").append(o).append("\n");
                        }
                    }
                    msg.append("\n提示：请到「课程列表」切换到目标轮次，完善教师/教室/时间后开放。");
                    JOptionPane.showMessageDialog(dialog, msg.toString(), "复制结果", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadRounds();
                    updateHeaderRound();
                    loadCourseList();
                } else {
                    JOptionPane.showMessageDialog(dialog, resp != null ? resp.getResponseMsg() : "未知错误", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "复制失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnPanel.add(btnOk);

        JButton btnCancel = new JButton("取消");
        btnCancel.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnCancel);

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private List<SelectRound> fetchAllRounds() {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_GET_ALL_ROUNDS);
            req.setData(null);
            Message resp = SocketClient.send(req);
            if (resp != null && resp.isSuccess() && resp.getData() != null) return (List<SelectRound>) resp.getData();
        } catch (Exception e) { e.printStackTrace(); }
        return new ArrayList<>();
    }

    private void showEditRoundDialog(SelectRound round) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "修改选课轮次", true);
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtRoundId = new JTextField(round.getRoundId(), 18);
        txtRoundId.setEditable(false);
        txtRoundId.setBackground(new Color(240, 240, 240));

        JTextField txtSemester = new JTextField(round.getSemester(), 18);
        JTextField txtRoundName = new JTextField(round.getRoundName(), 18);

        JTextField txtStartTime = new JTextField(round.getStartTime(), 18);
        JLabel lblStartHint = new JLabel("格式：yyyy-MM-dd HH:mm:ss（例：2025-09-11 19:04:00）");
        lblStartHint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        lblStartHint.setForeground(new Color(120, 120, 120));

        JTextField txtEndTime = new JTextField(round.getEndTime(), 18);
        JLabel lblEndHint = new JLabel("格式：yyyy-MM-dd HH:mm:ss（例：2025-09-20 23:59:00）");
        lblEndHint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        lblEndHint.setForeground(new Color(120, 120, 120));

        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"upcoming", "open", "closed"});
        if (round.getStatus() != null) cbStatus.setSelectedItem(round.getStatus());

        txtStartTime.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String s = txtStartTime.getText().trim();
                if (s.isEmpty()) {
                    lblStartHint.setText("格式：yyyy-MM-dd HH:mm:ss（例：2025-09-11 19:04:00）");
                    lblStartHint.setForeground(new Color(120, 120, 120));
                    return;
                }
                String norm = normalizeDateTime(s);
                if (norm == null) {
                    lblStartHint.setText("格式错误，应为 yyyy-MM-dd HH:mm:ss");
                    lblStartHint.setForeground(Color.RED);
                } else {
                    lblStartHint.setText("已识别：" + norm);
                    lblStartHint.setForeground(new Color(60, 150, 80));
                    txtStartTime.setText(norm);
                }
            }
        });

        txtEndTime.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                String s = txtEndTime.getText().trim();
                if (s.isEmpty()) {
                    lblEndHint.setText("格式：yyyy-MM-dd HH:mm:ss（例：2025-09-20 23:59:00）");
                    lblEndHint.setForeground(new Color(120, 120, 120));
                    return;
                }
                String norm = normalizeDateTime(s);
                if (norm == null) {
                    lblEndHint.setText("格式错误，应为 yyyy-MM-dd HH:mm:ss");
                    lblEndHint.setForeground(Color.RED);
                } else {
                    lblEndHint.setText("已识别：" + norm);
                    lblEndHint.setForeground(new Color(60, 150, 80));
                    txtEndTime.setText(norm);
                }
            }
        });

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("轮次ID："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtRoundId, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("学期："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtSemester, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("轮次名称："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtRoundName, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("开始时间："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtStartTime, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1; form.add(lblStartHint, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("结束时间："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(txtEndTime, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1; form.add(lblEndHint, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; form.add(new JLabel("状态："), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(cbStatus, gbc);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(Color.WHITE);

        JButton btnOk = new JButton("保存修改");
        btnOk.setBackground(BTN_GREEN);
        btnOk.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnOk.addActionListener(e -> {
            if (txtSemester.getText().trim().isEmpty()
                    || txtRoundName.getText().trim().isEmpty()
                    || txtStartTime.getText().trim().isEmpty()
                    || txtEndTime.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "请填写完整信息");
                return;
            }

            String startNorm = normalizeDateTime(txtStartTime.getText().trim());
            String endNorm = normalizeDateTime(txtEndTime.getText().trim());

            if (startNorm == null) {
                lblStartHint.setText("格式错误，应为 yyyy-MM-dd HH:mm:ss");
                lblStartHint.setForeground(Color.RED);
                JOptionPane.showMessageDialog(dialog, "开始时间格式错误");
                return;
            }
            if (endNorm == null) {
                lblEndHint.setText("格式错误，应为 yyyy-MM-dd HH:mm:ss");
                lblEndHint.setForeground(Color.RED);
                JOptionPane.showMessageDialog(dialog, "结束时间格式错误");
                return;
            }

            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.util.Date startDate = sdf.parse(startNorm);
                java.util.Date endDate = sdf.parse(endNorm);
                if (!startDate.before(endDate)) {
                    JOptionPane.showMessageDialog(dialog, "开始时间必须早于结束时间");
                    return;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "时间解析失败：" + ex.getMessage());
                return;
            }

            SelectRound updated = new SelectRound();
            updated.setRoundId(round.getRoundId());
            updated.setSemester(txtSemester.getText().trim());
            updated.setRoundName(txtRoundName.getText().trim());
            updated.setStartTime(startNorm);
            updated.setEndTime(endNorm);
            updated.setStatus((String) cbStatus.getSelectedItem());

            String result = updateRoundToServer(updated);
            if ("修改成功".equals(result)) {
                JOptionPane.showMessageDialog(dialog, "修改成功");
                dialog.dispose();
                loadRounds();
                updateHeaderRound();
                loadCourseList();
            } else {
                JOptionPane.showMessageDialog(dialog, result, "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnPanel.add(btnOk);

        JButton btnCancel = new JButton("取消");
        btnCancel.setBorder(new EmptyBorder(6, 25, 6, 25));
        btnCancel.addActionListener(e -> dialog.dispose());
        btnPanel.add(btnCancel);

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private String updateRoundToServer(SelectRound round) {
        try {
            Message req = new Message();
            req.setType(MsgConst.ADMIN_UPDATE_ROUND);
            req.setData(round);
            Message resp = SocketClient.send(req);
            return resp.getResponseMsg();
        } catch (Exception e) {
            e.printStackTrace();
            return "连接服务器失败：" + e.getMessage();
        }
    }
    /**
     * 管理员端轮询：定期刷新轮次卡片和课程列表
     * 不打断用户当前操作（学生搜索/编辑弹窗等）
     */
    private void startAdminPolling() {
        if (pollTimer != null) pollTimer.stop();
        pollTimer = new javax.swing.Timer(POLL_INTERVAL_MS, e -> {
            // 只在"课程列表"卡片显示时才刷，避免打断正在编辑的对话框
            // 你可以根据 contentCardLayout 当前显示的卡判断，但为简单起见：只要不 busy 就刷
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    // 只拉数据，不碰 UI
                    return null;
                }
                @Override
                protected void done() {
                    // 刷新轮次卡片 + 课程列表
                    // 注意：loadRounds 里会重建 roundsPanel，不影响正在编辑的 dialog
                    loadRounds();
                    loadCourseList();
                }
            }.execute();
        });
        pollTimer.setRepeats(true);
        pollTimer.start();
    }
}