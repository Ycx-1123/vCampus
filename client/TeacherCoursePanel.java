package vCampus.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vCampus.common.Message;
import vCampus.common.consts.MsgConst;
import vCampus.common.vo.Teacher;
import vCampus.common.vo.CourseDisplayDTO;
import vCampus.common.vo.StudentScoreDTO;
import vCampus.common.vo.SelectRound;

/**
 * 教师课程管理面板
 * 功能：卡片式展示授课课程，点击进入学生名单视图
 */
public class TeacherCoursePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private Object currentUser;
    private String teacherCard;
    private javax.swing.Timer pollTimer;
    private static final int POLL_INTERVAL_MS = 5000;

    // 顶部
    private JComboBox<String> cbSemester;
    private JButton btnRefresh;
    private JButton btnBack;
    private JLabel lblTitle;

    // 视图切换
    private JPanel centerContainer;
    private CardLayout centerCardLayout;
    private JPanel courseListView;
    private JPanel courseListPanel;
    private JPanel studentListView;

    // 学生名单视图组件
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JButton btnViewSchedule;
    private JButton btnGradeManage;
    private JButton btnExport;
    private JLabel lblStatus;

    private static final Color BG_COLOR = new Color(210, 230, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color PANEL_BG = new Color(180, 210, 240);
    private static final Color TABLE_BG = new Color(230, 242, 255);
    private static final Color BTN_GREEN = new Color(183, 220, 187);
    private static final Color BTN_BLUE = new Color(150, 200, 240);
    private static final Color BTN_ORANGE = new Color(240, 200, 150);
    private static final Color BORDER_COLOR = new Color(180, 210, 240);
    private static final Color HEADER_BG = new Color(235, 245, 240);

    private static final int COURSE_CARD_HEIGHT = 130;

    private List<CourseDisplayDTO> myCourses = new ArrayList<>();
    private String currentSemester;
    private String currentOfferingId;
    private String currentCourseName;

    public TeacherCoursePanel(Object user) {
        this.currentUser = user;
        if (user instanceof Teacher) {
            this.teacherCard = ((Teacher) user).getCardNumber();
        }
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        initUI();

        loadSemesters();
        loadCoursesFromServer();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    loadSemesters();
                    loadCoursesFromServer();
                });
            }
        });

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    SwingUtilities.invokeLater(() -> {
                        loadSemesters();
                        loadCoursesFromServer();
                    });
                }
            }
        });
        startPolling();
    }

    private void initUI() {
        // ========== 顶部 ==========
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(new EmptyBorder(15, 25, 15, 25));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setBackground(Color.WHITE);

        btnBack = new JButton("←");
        btnBack.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        btnBack.setForeground(Color.BLACK);
        btnBack.setBackground(Color.WHITE);
        btnBack.setBorder(new EmptyBorder(0, 5, 0, 5));
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBack.setFocusPainted(false);
        btnBack.setContentAreaFilled(false);
        btnBack.setVisible(false);
        btnBack.addActionListener(e -> showListView());
        leftPanel.add(btnBack);

        lblTitle = new JLabel("课程管理");
        lblTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        lblTitle.setForeground(Color.BLACK);
        leftPanel.add(lblTitle);

        topPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Color.WHITE);

        rightPanel.add(new JLabel("学期："));
        cbSemester = new JComboBox<>();
        cbSemester.setPreferredSize(new Dimension(140, 30));
        cbSemester.addActionListener(e -> {
            String selected = (String) cbSemester.getSelectedItem();
            if (selected != null && !"暂无学期".equals(selected)) {
                currentSemester = selected;
                loadCoursesFromServer();
            }
        });
        rightPanel.add(cbSemester);

        btnRefresh = new JButton("刷新");
        btnRefresh.setBackground(BTN_GREEN);
        btnRefresh.setBorder(new EmptyBorder(6, 16, 6, 16));
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> {
            loadSemesters();
            loadCoursesFromServer();
        });
        rightPanel.add(btnRefresh);

        topPanel.add(rightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ========== 中部：两个视图 ==========
        centerCardLayout = new CardLayout();
        centerContainer = new JPanel(centerCardLayout);
        centerContainer.setBackground(BG_COLOR);

        // 视图1：课程列表（卡片式）
        courseListView = new JPanel(new BorderLayout());
        courseListView.setBackground(BG_COLOR);
        courseListView.setBorder(new EmptyBorder(10, 25, 10, 25));

        courseListPanel = new JPanel();
        courseListPanel.setLayout(new BoxLayout(courseListPanel, BoxLayout.Y_AXIS));
        courseListPanel.setBackground(BG_COLOR);

        JScrollPane courseScroll = new JScrollPane(courseListPanel);
        courseScroll.getVerticalScrollBar().setUnitIncrement(16);
        courseScroll.setBorder(null);
        courseScroll.getViewport().setBackground(BG_COLOR);

        courseListView.add(courseScroll, BorderLayout.CENTER);

        // 视图2：学生名单（原来的界面）
        studentListView = createStudentListView();

        centerContainer.add(courseListView, "courseList");
        centerContainer.add(studentListView, "studentList");

        add(centerContainer, BorderLayout.CENTER);
    }

    private JPanel createStudentListView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);

        // 学生名单表格
        String[] columns = {"学号", "姓名", "专业", "班级", "首修/重修", "状态"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        studentTable = new JTable(tableModel);
        studentTable.setRowHeight(34);
        studentTable.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        studentTable.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        studentTable.setBackground(TABLE_BG);
        studentTable.setForeground(new Color(40, 40, 40));
        studentTable.getTableHeader().setBackground(new Color(190, 215, 240));
        studentTable.getTableHeader().setForeground(new Color(40, 40, 40));

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(150, 185, 215), 1));
        scrollPane.setBackground(PANEL_BG);
        scrollPane.getViewport().setBackground(TABLE_BG);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BG_COLOR);
        centerPanel.setBorder(new EmptyBorder(10, 25, 10, 25));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        // 底部按钮
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(10, 25, 15, 25));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(Color.WHITE);
        lblStatus = new JLabel("共 0 名学生");
        lblStatus.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblStatus.setForeground(new Color(100, 100, 100));
        leftPanel.add(lblStatus);
        bottomPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Color.WHITE);

        btnExport = new JButton("导出学生名单");
        btnExport.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        btnExport.setBackground(new Color(200, 220, 240));
        btnExport.setBorder(new EmptyBorder(8, 16, 8, 16));
        btnExport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExport.addActionListener(e -> exportStudentList());
        rightPanel.add(btnExport);

        btnGradeManage = new JButton("成绩管理");
        btnGradeManage.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        btnGradeManage.setBackground(BTN_ORANGE);
        btnGradeManage.setForeground(new Color(60, 40, 20));
        btnGradeManage.setBorder(new EmptyBorder(8, 20, 8, 20));
        btnGradeManage.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGradeManage.addActionListener(e -> showGradeManageDialog());
        rightPanel.add(btnGradeManage);

        btnViewSchedule = new JButton("查看我的课表");
        btnViewSchedule.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        btnViewSchedule.setBackground(BTN_BLUE);
        btnViewSchedule.setForeground(new Color(40, 60, 80));
        btnViewSchedule.setBorder(new EmptyBorder(8, 20, 8, 20));
        btnViewSchedule.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewSchedule.addActionListener(e -> showScheduleDialog());
        rightPanel.add(btnViewSchedule);

        bottomPanel.add(rightPanel, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ============================================================
    // 视图切换
    // ============================================================
    private void showListView() {
        btnBack.setVisible(false);
        lblTitle.setText("课程管理");
        centerCardLayout.show(centerContainer, "courseList");
    }

    private void showStudentListView(CourseDisplayDTO course) {
        currentOfferingId = course.getOfferingId();
        currentCourseName = course.getCourseName() + " (" + course.getCourseId() + ")";
        btnBack.setVisible(true);
        lblTitle.setText(currentCourseName);
        loadStudentList();
        centerCardLayout.show(centerContainer, "studentList");
    }

    // ============================================================
    // 加载学期
    // ============================================================
    private void loadSemesters() {
        if (teacherCard == null || teacherCard.isEmpty()) return;

        java.awt.event.ActionListener[] listeners = cbSemester.getActionListeners();
        for (java.awt.event.ActionListener l : listeners) cbSemester.removeActionListener(l);

        cbSemester.removeAllItems();
        List<String> semesters = new ArrayList<>();

        try {
            Message request = new Message();
            request.setType(MsgConst.TEACHER_GET_MY_SEMESTERS);
            request.setData(teacherCard);
            Message response = SocketClient.send(request);

            if (response != null && response.isSuccess() && response.getData() != null) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) response.getData();
                semesters.addAll(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(">>> [loadSemesters] teacherCard=" + teacherCard + ", semesters=" + semesters);

        if (semesters.isEmpty()) {
            cbSemester.addItem("暂无学期");
            cbSemester.setEnabled(false);
            currentSemester = null;
        } else {
            for (String s : semesters) cbSemester.addItem(s);
            cbSemester.setEnabled(true);

            // 默认选第一个（后端已按学期倒序，第一个就是最新的）
            cbSemester.setSelectedIndex(0);
            currentSemester = (String) cbSemester.getSelectedItem();
        }

        for (java.awt.event.ActionListener l : listeners) cbSemester.addActionListener(l);
    }

    // ============================================================
    // 加载课程列表（卡片式）
    // ============================================================
    private void loadCoursesFromServer() {
        if (teacherCard == null || teacherCard.isEmpty()) return;
        if (currentSemester == null || currentSemester.isEmpty()) {
            if (cbSemester.getItemCount() == 0) {
                loadSemesters();
                return;
            }
            currentSemester = (String) cbSemester.getSelectedItem();
            if (currentSemester == null || "暂无学期".equals(currentSemester)) return;
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("teacherId", teacherCard);
            params.put("semester", currentSemester);

            Message request = new Message();
            request.setType(MsgConst.TEACHER_GET_MY_COURSES);
            request.setData(params);
            Message response = SocketClient.send(request);

            if (response != null && response.isSuccess() && response.getData() != null) {
                @SuppressWarnings("unchecked")
                List<CourseDisplayDTO> courses = (List<CourseDisplayDTO>) response.getData();
                myCourses = courses;
                renderCourseCards();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderCourseCards() {
        courseListPanel.removeAll();

        if (myCourses.isEmpty()) {
            JLabel empty = new JLabel("该学期暂无授课课程", SwingConstants.CENTER);
            empty.setFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
            empty.setForeground(new Color(120, 140, 130));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            courseListPanel.add(Box.createVerticalStrut(60));
            courseListPanel.add(empty);
        } else {
            for (CourseDisplayDTO course : myCourses) {
                JPanel card = createCourseCard(course);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, COURSE_CARD_HEIGHT));
                card.setPreferredSize(new Dimension(900, COURSE_CARD_HEIGHT));
                courseListPanel.add(card);
                courseListPanel.add(Box.createVerticalStrut(12));
            }
        }

        courseListPanel.revalidate();
        courseListPanel.repaint();
    }

    private JPanel createCourseCard(CourseDisplayDTO course) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(0, 0, 0, 0)));

        // 左侧色条
        JPanel leftBar = new JPanel();
        leftBar.setPreferredSize(new Dimension(6, 0));
        leftBar.setBackground(new Color(70, 110, 180));
        card.add(leftBar, BorderLayout.WEST);

        // 中间信息区
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(CARD_BG);
        infoPanel.setBorder(new EmptyBorder(15, 25, 15, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 20);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 第一行：课程号 + 课程名
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblId = new JLabel(course.getCourseId());
        lblId.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        lblId.setForeground(new Color(60, 100, 160));
        infoPanel.add(lblId, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        JLabel lblName = new JLabel(course.getCourseName());
        lblName.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        lblName.setForeground(new Color(40, 40, 40));
        infoPanel.add(lblName, gbc);

        // 第二行：学分 + 教师
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1;
        JLabel lblInfo1 = new JLabel(course.getCredit() + " 学分      教师：" + str(course.getTeacherName()));
        lblInfo1.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblInfo1.setForeground(new Color(80, 80, 80));
        infoPanel.add(lblInfo1, gbc);

        // 第三行：时间
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel lblTime = new JLabel("时间：" + str(course.getSchedule()));
        lblTime.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblTime.setForeground(new Color(100, 100, 100));
        infoPanel.add(lblTime, gbc);

        // 第四行：教室 + 校区
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JLabel lblRoom = new JLabel("教室：" + str(course.getClassroom())
                + "      校区：" + str(course.getCampus()));
        lblRoom.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblRoom.setForeground(new Color(100, 100, 100));
        infoPanel.add(lblRoom, gbc);

        card.add(infoPanel, BorderLayout.CENTER);

        // 右侧按钮区
        JPanel btnPanel = new JPanel(new GridBagLayout());
        btnPanel.setBackground(CARD_BG);
        btnPanel.setBorder(new EmptyBorder(0, 10, 0, 25));

        JButton btnView = new JButton("查看课程信息");
        btnView.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        btnView.setPreferredSize(new Dimension(130, 44));
        btnView.setBorder(new EmptyBorder(8, 20, 8, 20));
        btnView.setFocusPainted(false);

        // ★ 直接用 DTO 里的 roundStatus 判断
        String rs = course.getRoundStatus();
        boolean roundOpen = "open".equals(rs) || "upcoming".equals(rs);

        if (roundOpen) {
            btnView.setText("选课中");
            btnView.setBackground(new Color(220, 220, 220));
            btnView.setForeground(new Color(150, 150, 150));
            btnView.setCursor(Cursor.getDefaultCursor());
            btnView.setEnabled(false);
            btnView.setToolTipText("选课轮次开放中，结束后才能查看学生名单");
        } else {
            btnView.setText("查看课程信息");
            btnView.setBackground(BTN_BLUE);
            btnView.setForeground(new Color(40, 60, 80));
            btnView.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnView.addActionListener(e -> showStudentListView(course));
        }
        btnPanel.add(btnView);

        card.add(btnPanel, BorderLayout.EAST);

        return card;
    }
    
    // ============================================================
    // 加载学生名单
    // ============================================================
    private void loadStudentList() {
        tableModel.setRowCount(0);
        if (currentOfferingId == null) return;

        List<StudentScoreDTO> students = loadStudentsFromServer(currentOfferingId);
        for (StudentScoreDTO s : students) {
            tableModel.addRow(new Object[]{
                    s.getStudentId(),
                    s.getStudentName(),
                    s.getMajor() != null ? s.getMajor() : "",
                    s.getClassName() != null ? s.getClassName() : "",
                    "first".equals(s.getSelectType()) ? "首修" : "重修",
                    s.getStatus()
            });
        }
        lblStatus.setText("共 " + students.size() + " 名学生");
    }

    private List<StudentScoreDTO> loadStudentsFromServer(String offeringId) {
        try {
            Message request = new Message();
            request.setType(MsgConst.TEACHER_GET_STUDENTS);
            request.setData(offeringId);
            Message response = SocketClient.send(request);

            if (response != null && response.isSuccess() && response.getData() != null) {
                @SuppressWarnings("unchecked")
                List<StudentScoreDTO> students = (List<StudentScoreDTO>) response.getData();
                return students;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // ============================================================
    // 导出学生名单
    // ============================================================
    private void exportStudentList() {
        if (currentOfferingId == null || currentOfferingId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择一门课程", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<StudentScoreDTO> students = loadStudentsFromServer(currentOfferingId);
        if (students.isEmpty()) {
            JOptionPane.showMessageDialog(this, "该课程暂无学生", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        String fileName = currentCourseName != null ? currentCourseName.replace(" ", "_") : "学生名单";
        fileChooser.setSelectedFile(new File(fileName + ".xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel文件 (*.xlsx)", "xlsx"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fileChooser.getSelectedFile();
        if (!file.getName().endsWith(".xlsx")) {
            file = new File(file.getAbsolutePath() + ".xlsx");
        }

        try {
            exportToExcel(file, students);
            JOptionPane.showMessageDialog(this,
                    "导出成功！文件保存在：" + file.getAbsolutePath() +
                            "\n请填写成绩列，然后使用成绩管理中的导入Excel功能批量录入。",
                    "导出成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "导出失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportToExcel(File file, List<StudentScoreDTO> students) throws Exception {
        org.apache.poi.ss.usermodel.Workbook workbook = null;
        FileOutputStream fos = null;
        try {
            workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("学生名单");

            String[] headers = {"学号", "姓名", "专业", "班级", "首修/重修", "成绩"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
                style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                cell.setCellStyle(style);
            }

            int rowNum = 1;
            for (StudentScoreDTO s : students) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.getStudentId() != null ? s.getStudentId() : "");
                row.createCell(1).setCellValue(s.getStudentName() != null ? s.getStudentName() : "");
                row.createCell(2).setCellValue(s.getMajor() != null ? s.getMajor() : "");
                row.createCell(3).setCellValue(s.getClassName() != null ? s.getClassName() : "");
                row.createCell(4).setCellValue("first".equals(s.getSelectType()) ? "首修" : "重修");
                if (s.getScore() != null) {
                    row.createCell(5).setCellValue(s.getScore());
                } else {
                    row.createCell(5).setCellValue("");
                }
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.flush();
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException e) { e.printStackTrace(); }
            if (workbook != null) try { workbook.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    // ============================================================
    // 成绩管理弹窗
    // ============================================================
    private void showGradeManageDialog() {
        if (currentOfferingId == null || currentOfferingId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择一门课程", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String courseName = currentCourseName;
        if (courseName == null) {
            JOptionPane.showMessageDialog(this, "请先选择一门课程", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<StudentScoreDTO> students = loadStudentsFromServer(currentOfferingId);
        if (students.isEmpty()) {
            JOptionPane.showMessageDialog(this, "该课程暂无学生", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "成绩管理 - " + courseName, true);
        dialog.setSize(950, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // 顶部：搜索 + 批量操作
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setBackground(Color.WHITE);
        JTextField txtSearch = new JTextField(20);
        JButton btnSearch = new JButton("搜索");
        btnSearch.setBackground(BTN_GREEN);
        btnSearch.setBorder(new EmptyBorder(4, 16, 4, 16));
        JButton btnClear = new JButton("清空");
        btnClear.setBorder(new EmptyBorder(4, 16, 4, 16));
        searchPanel.add(new JLabel("搜索："));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnClear);
        topPanel.add(searchPanel, BorderLayout.WEST);

        JPanel batchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        batchPanel.setBackground(Color.WHITE);
        JButton btnImportExcel = new JButton("导入Excel");
        btnImportExcel.setBackground(new Color(150, 200, 150));
        btnImportExcel.setBorder(new EmptyBorder(4, 16, 4, 16));
        JButton btnBatchSave = new JButton("批量保存");
        btnBatchSave.setBackground(new Color(150, 180, 220));
        btnBatchSave.setBorder(new EmptyBorder(4, 16, 4, 16));
        JButton btnRefreshStats = new JButton("刷新统计");
        btnRefreshStats.setBackground(new Color(200, 200, 200));
        btnRefreshStats.setBorder(new EmptyBorder(4, 16, 4, 16));
        batchPanel.add(btnImportExcel);
        batchPanel.add(btnBatchSave);
        batchPanel.add(btnRefreshStats);
        topPanel.add(batchPanel, BorderLayout.EAST);

        // 学生表格
        String[] cols = {"学号", "姓名", "专业", "首修/重修", "成绩", "状态", "保存"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.getTableHeader().setBackground(HEADER_BG);
        table.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        table.setBackground(TABLE_BG);

        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);

        List<StudentScoreDTO> studentList = new ArrayList<>(students);
        Map<String, StudentScoreDTO> studentMap = new HashMap<>();
        for (StudentScoreDTO s : students) {
            studentMap.put(s.getStudentId(), s);
            model.addRow(new Object[]{
                    s.getStudentId(), s.getStudentName(),
                    s.getMajor() != null ? s.getMajor() : "",
                    "first".equals(s.getSelectType()) ? "首修" : "重修",
                    s.getScore() != null ? s.getScore() : "",
                    s.getStatus(), "保存"
            });
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(150, 185, 215), 1));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BG_COLOR);
        centerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // 底部统计面板
        JPanel statsPanel = new JPanel(new GridBagLayout());
        statsPanel.setBackground(new Color(240, 248, 255));
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 185, 215), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JLabel lblTotalStudents = new JLabel("总人数: 0");
        JLabel lblScoredCount = new JLabel("已录入: 0");
        JLabel lblUnscoredCount = new JLabel("未录入: 0");
        JLabel lblAvgScore = new JLabel("平均分: 0.0");
        JLabel lblMaxScore = new JLabel("最高分: 0");
        JLabel lblMinScore = new JLabel("最低分: 0");
        JLabel lblPassCount = new JLabel("及格: 0");
        JLabel lblFailCount = new JLabel("不及格: 0");
        JLabel lblPassRate = new JLabel("及格率: 0%");
        JLabel lblExcellentRate = new JLabel("优秀率: 0%");

        Font statsFont = new Font("Microsoft YaHei", Font.PLAIN, 13);
        Color statsColor = new Color(40, 60, 80);
        JLabel[] labels = {lblTotalStudents, lblScoredCount, lblUnscoredCount, lblAvgScore,
                lblMaxScore, lblMinScore, lblPassCount, lblFailCount, lblPassRate, lblExcellentRate};
        for (JLabel l : labels) {
            l.setFont(statsFont);
            l.setForeground(statsColor);
        }

        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(2, 10, 2, 10);
        sgbc.gridx = 0; sgbc.gridy = 0; statsPanel.add(lblTotalStudents, sgbc);
        sgbc.gridx = 1; statsPanel.add(lblScoredCount, sgbc);
        sgbc.gridx = 2; statsPanel.add(lblUnscoredCount, sgbc);
        sgbc.gridx = 3; statsPanel.add(lblAvgScore, sgbc);
        sgbc.gridx = 4; statsPanel.add(lblMaxScore, sgbc);
        sgbc.gridx = 0; sgbc.gridy = 1; statsPanel.add(lblMinScore, sgbc);
        sgbc.gridx = 1; statsPanel.add(lblPassCount, sgbc);
        sgbc.gridx = 2; statsPanel.add(lblFailCount, sgbc);
        sgbc.gridx = 3; statsPanel.add(lblPassRate, sgbc);
        sgbc.gridx = 4; statsPanel.add(lblExcellentRate, sgbc);

        Runnable updateStats = () -> {
            int total = studentList.size();
            int scored = 0;
            double sum = 0, max = 0, min = 100;
            int pass = 0, fail = 0, excellent = 0;
            for (StudentScoreDTO s : studentList) {
                if (s.getScore() != null) {
                    scored++;
                    double sc = s.getScore();
                    sum += sc;
                    if (sc > max) max = sc;
                    if (sc < min) min = sc;
                    if (sc >= 60) pass++;
                    if (sc < 60) fail++;
                    if (sc >= 90) excellent++;
                }
            }
            lblTotalStudents.setText("总人数: " + total);
            lblScoredCount.setText("已录入: " + scored);
            lblUnscoredCount.setText("未录入: " + (total - scored));
            lblAvgScore.setText("平均分: " + (scored > 0 ? String.format("%.1f", sum / scored) : "0.0"));
            lblMaxScore.setText("最高分: " + (scored > 0 ? String.format("%.1f", max) : "0"));
            lblMinScore.setText("最低分: " + (scored > 0 ? String.format("%.1f", min) : "0"));
            lblPassCount.setText("及格: " + pass);
            lblFailCount.setText("不及格: " + fail);
            lblPassRate.setText("及格率: " + (scored > 0 ? String.format("%.1f", pass * 100.0 / scored) : "0") + "%");
            lblExcellentRate.setText("优秀率: " + (scored > 0 ? String.format("%.1f", excellent * 100.0 / scored) : "0") + "%");
        };

        Runnable refreshData = () -> {
            List<StudentScoreDTO> refreshed = loadStudentsFromServer(currentOfferingId);
            studentList.clear();
            studentList.addAll(refreshed);
            studentMap.clear();
            for (StudentScoreDTO s : refreshed) {
                studentMap.put(s.getStudentId(), s);
            }
            model.setRowCount(0);
            for (StudentScoreDTO s : refreshed) {
                model.addRow(new Object[]{
                        s.getStudentId(), s.getStudentName(),
                        s.getMajor() != null ? s.getMajor() : "",
                        "first".equals(s.getSelectType()) ? "首修" : "重修",
                        s.getScore() != null ? s.getScore() : "",
                        s.getStatus(), "保存"
                });
            }
            updateStats.run();
        };

        updateStats.run();

        // 底部关闭按钮
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        JLabel lblTotal = new JLabel("共 " + students.size() + " 名学生");
        lblTotal.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        bottomPanel.add(lblTotal, BorderLayout.WEST);
        JButton btnClose = new JButton("关闭");
        btnClose.setBackground(new Color(200, 200, 200));
        btnClose.setBorder(new EmptyBorder(6, 20, 6, 20));
        btnClose.addActionListener(e -> dialog.dispose());
        bottomPanel.add(btnClose, BorderLayout.EAST);

        dialog.add(topPanel, BorderLayout.NORTH);
        dialog.add(centerPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(statsPanel, BorderLayout.CENTER);
        southPanel.add(bottomPanel, BorderLayout.SOUTH);
        dialog.add(southPanel, BorderLayout.SOUTH);

        // 搜索
        btnSearch.addActionListener(e -> {
            String keyword = txtSearch.getText().trim().toLowerCase();
            model.setRowCount(0);
            for (StudentScoreDTO s : studentList) {
                if (keyword.isEmpty()
                        || (s.getStudentId() != null && s.getStudentId().toLowerCase().contains(keyword))
                        || (s.getStudentName() != null && s.getStudentName().toLowerCase().contains(keyword))
                        || (s.getMajor() != null && s.getMajor().toLowerCase().contains(keyword))
                        || (s.getClassName() != null && s.getClassName().toLowerCase().contains(keyword))) {
                    model.addRow(new Object[]{
                            s.getStudentId(), s.getStudentName(),
                            s.getMajor() != null ? s.getMajor() : "",
                            "first".equals(s.getSelectType()) ? "首修" : "重修",
                            s.getScore() != null ? s.getScore() : "",
                            s.getStatus(), "保存"
                    });
                }
            }
        });

        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            btnSearch.doClick();
        });

        btnRefreshStats.addActionListener(e -> refreshData.run());

        // 单个保存
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int col = table.getSelectedColumn();
                int row = table.getSelectedRow();
                if (col == 6 && row >= 0) {
                    String sid = (String) table.getValueAt(row, 0);
                    String sname = (String) table.getValueAt(row, 1);
                    Object so = table.getValueAt(row, 4);
                    String ss = so != null ? so.toString().trim() : "";
                    if (ss.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "请输入成绩");
                        return;
                    }
                    try {
                        double score = Double.parseDouble(ss);
                        if (score < 0 || score > 100) {
                            JOptionPane.showMessageDialog(dialog, "成绩必须在 0-100 之间");
                            return;
                        }
                        if (saveScoreToServer(currentOfferingId, sid, score)) {
                            JOptionPane.showMessageDialog(dialog, sname + " 成绩保存成功");
                            refreshData.run();
                        } else {
                            JOptionPane.showMessageDialog(dialog, "保存失败");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(dialog, "请输入有效数字");
                    }
                }
            }
        });

        // 批量保存
        btnBatchSave.addActionListener(e -> {
            int saved = 0;
            List<String> errors = new ArrayList<>();
            for (int i = 0; i < table.getRowCount(); i++) {
                String sid = (String) table.getValueAt(i, 0);
                String sname = (String) table.getValueAt(i, 1);
                Object so = table.getValueAt(i, 4);
                if (so != null && !so.toString().trim().isEmpty()) {
                    try {
                        double score = Double.parseDouble(so.toString().trim());
                        if (score < 0 || score > 100) {
                            errors.add(sname + ": 超出范围");
                            continue;
                        }
                        if (saveScoreToServer(currentOfferingId, sid, score)) {
                            saved++;
                        } else {
                            errors.add(sname + ": 保存失败");
                        }
                    } catch (NumberFormatException ex) {
                        errors.add(sname + ": 格式错误");
                    }
                }
            }
            String msg = "批量保存完成，成功 " + saved + " 条";
            if (!errors.isEmpty()) msg += "\n失败：" + String.join("; ", errors);
            JOptionPane.showMessageDialog(dialog, msg);
            refreshData.run();
        });

        // 导入 Excel
        btnImportExcel.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Excel文件 (*.xlsx, *.xls)", "xlsx", "xls"));
            if (fc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                importExcelScores(fc.getSelectedFile(), studentMap, dialog);
                refreshData.run();
            }
        });

        dialog.setVisible(true);
    }

    private void importExcelScores(File file, Map<String, StudentScoreDTO> studentMap, JDialog parent) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook;
            try (FileInputStream fis = new FileInputStream(file)) {
                if (file.getName().endsWith(".xlsx")) {
                    workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(fis);
                } else {
                    workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook(fis);
                }
            }

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            int imported = 0, fail = 0;
            List<String> notFound = new ArrayList<>();

            int nameCol = -1, scoreCol = -1, headerRow = 0;
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
                    if (cell != null) {
                        String v = cell.getStringCellValue().trim();
                        if (v.contains("姓名")) nameCol = i;
                        if (v.contains("成绩")) scoreCol = i;
                    }
                }
                if (nameCol >= 0 && scoreCol >= 0) {
                    headerRow = row.getRowNum();
                    break;
                }
            }

            if (nameCol < 0 || scoreCol < 0) {
                JOptionPane.showMessageDialog(parent, "未找到姓名和成绩列");
                workbook.close();
                return;
            }

            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                if (row.getRowNum() <= headerRow) continue;
                org.apache.poi.ss.usermodel.Cell nameCell = row.getCell(nameCol);
                org.apache.poi.ss.usermodel.Cell scoreCell = row.getCell(scoreCol);
                if (nameCell == null) continue;
                String name = nameCell.getStringCellValue().trim();
                if (name.isEmpty()) continue;

                double score;
                try {
                    if (scoreCell == null) { fail++; continue; }
                    if (scoreCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        score = scoreCell.getNumericCellValue();
                    } else {
                        score = Double.parseDouble(scoreCell.getStringCellValue().trim());
                    }
                } catch (NumberFormatException ex) {
                    fail++;
                    continue;
                }
                if (score < 0 || score > 100) { fail++; continue; }

                StudentScoreDTO matched = null;
                for (StudentScoreDTO s : studentMap.values()) {
                    if (s.getStudentName().equals(name)) {
                        matched = s;
                        break;
                    }
                }

                if (matched != null) {
                    if (saveScoreToServer(currentOfferingId, matched.getStudentId(), score)) {
                        matched.setScore(score);
                        imported++;
                    } else fail++;
                } else {
                    notFound.add(name);
                    fail++;
                }
            }
            workbook.close();

            String msg = "导入完成，成功 " + imported + " 条";
            if (fail > 0) msg += "\n失败 " + fail + " 条";
            if (!notFound.isEmpty()) msg += "\n未找到：" + String.join("、", notFound);
            JOptionPane.showMessageDialog(parent, msg);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "导入失败：" + e.getMessage());
        }
    }

    private boolean saveScoreToServer(String offeringId, String studentId, double score) {
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
    // 查看课表
    // ============================================================
    private void showScheduleDialog() {
        if (myCourses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "您暂无授课课程，课表为空。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "我的课表", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel schedulePanel = new JPanel(new GridBagLayout());
        schedulePanel.setBackground(Color.WHITE);
        schedulePanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        String[] days = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        String[] periods = {"第1节", "第2节", "第3节", "第4节", "第5节", "第6节", "第7节", "第8节", "第9节", "第10节"};
        JLabel[][] cells = new JLabel[11][8];

        for (int row = 0; row < 11; row++) {
            for (int col = 0; col < 8; col++) {
                JLabel cell = new JLabel();
                cell.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
                cell.setOpaque(true);
                cell.setBackground(Color.WHITE);
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                cell.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));

                if (row == 0 && col == 0) {
                    cell.setText("时间/星期");
                    cell.setBackground(new Color(240, 240, 240));
                } else if (row == 0) {
                    cell.setText(days[col]);
                    cell.setBackground(new Color(240, 240, 240));
                    cell.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
                } else if (col == 0) {
                    cell.setText(periods[row - 1]);
                    cell.setBackground(new Color(240, 240, 240));
                } else {
                    cell.setBackground(new Color(250, 250, 250));
                }
                gbc.gridx = col;
                gbc.gridy = row;
                schedulePanel.add(cell, gbc);
                cells[row][col] = cell;
            }
        }

        Map<String, List<int[]>> courseCellMap = new LinkedHashMap<>();
        for (CourseDisplayDTO course : myCourses) {
            int[] p = parseSchedule(course.getSchedule());
            if (p == null) continue;
            String key = course.getOfferingId();
            courseCellMap.putIfAbsent(key, new ArrayList<>());
            for (int i = p[2]; i <= p[3] && i <= 10; i++) {
                courseCellMap.get(key).add(new int[]{i, p[1]});
            }
        }

        for (CourseDisplayDTO course : myCourses) {
            String key = course.getOfferingId();
            List<int[]> cellPos = courseCellMap.get(key);
            if (cellPos == null || cellPos.isEmpty()) continue;

            Color color = getCourseColor(course.getCourseId());
            for (int i = 0; i < cellPos.size(); i++) {
                int[] pos = cellPos.get(i);
                JLabel cell = cells[pos[0]][pos[1]];
                if (i == 0) {
                    String room = course.getClassroom() != null ? course.getClassroom() : "";
                    cell.setText("<html><center>" + course.getCourseName() + "<br>" + room + "</center></html>");
                } else {
                    cell.setText("");
                }
                cell.setBackground(color);
                cell.setForeground(Color.WHITE);
                cell.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
                cell.setBorder(BorderFactory.createEmptyBorder());
            }

            int[] first = cellPos.get(0);
            int[] last = cellPos.get(cellPos.size() - 1);
            cells[first[0]][first[1]].setBorder(cellPos.size() == 1
                    ? BorderFactory.createLineBorder(new Color(200, 200, 200), 1)
                    : BorderFactory.createMatteBorder(1, 1, 0, 1, new Color(200, 200, 200)));
            if (cellPos.size() > 1) {
                cells[last[0]][last[1]].setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, new Color(200, 200, 200)));
            }
            for (int i = 1; i < cellPos.size() - 1; i++) {
                int[] pos = cellPos.get(i);
                cells[pos[0]][pos[1]].setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, new Color(200, 200, 200)));
            }
        }

        JScrollPane sp = new JScrollPane(schedulePanel);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel info = new JLabel("共 " + myCourses.size() + " 门课程");
        info.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        bottom.add(info);

        JButton close = new JButton("关闭");
        close.setBackground(new Color(100, 180, 120));
        close.setForeground(Color.WHITE);
        close.setBorder(new EmptyBorder(6, 20, 6, 20));
        close.addActionListener(e -> dialog.dispose());
        bottom.add(close);

        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private int[] parseSchedule(String schedule) {
        if (schedule == null || schedule.isEmpty()) return null;
        String dayStr = null;
        String periodStr = null;

        if (schedule.contains(" ")) {
            String[] parts = schedule.split("\\s+");
            if (parts.length >= 2) {
                dayStr = parts[0];
                periodStr = parts[1];
            }
        }
        if (dayStr == null) {
            String[] weekDays = {"周一","周二","周三","周四","周五","周六","周日",
                    "星期一","星期二","星期三","星期四","星期五","星期六","星期日"};
            for (String wd : weekDays) {
                if (schedule.contains(wd)) {
                    dayStr = wd;
                    String rem = schedule.substring(schedule.indexOf(wd) + wd.length()).trim();
                    if (rem.startsWith("第")) rem = rem.substring(1);
                    periodStr = rem;
                    break;
                }
            }
        }
        if (dayStr == null || periodStr == null) return null;

        int dayIdx = getDayIndex(dayStr);
        if (dayIdx < 0) return null;
        int[] p = parsePeriods(periodStr);
        if (p == null) return null;
        return new int[]{dayIdx, dayIdx, p[0], p[1]};
    }

    private int getDayIndex(String dayStr) {
        if (dayStr == null) return -1;
        if (dayStr.contains("周一") || dayStr.contains("星期一") || dayStr.contains("周1")) return 1;
        if (dayStr.contains("周二") || dayStr.contains("星期二") || dayStr.contains("周2")) return 2;
        if (dayStr.contains("周三") || dayStr.contains("星期三") || dayStr.contains("周3")) return 3;
        if (dayStr.contains("周四") || dayStr.contains("星期四") || dayStr.contains("周4")) return 4;
        if (dayStr.contains("周五") || dayStr.contains("星期五") || dayStr.contains("周5")) return 5;
        if (dayStr.contains("周六") || dayStr.contains("星期六") || dayStr.contains("周6")) return 6;
        if (dayStr.contains("周日") || dayStr.contains("星期日") || dayStr.contains("周7")) return 7;
        return -1;
    }

    private int[] parsePeriods(String periodStr) {
        if (periodStr == null) return null;
        String cleaned = periodStr.replace("节", "").replace("第", "").trim();
        if (cleaned.contains("-") || cleaned.contains("—") || cleaned.contains("–")) {
            String sep = cleaned.contains("—") ? "—" : (cleaned.contains("–") ? "–" : "-");
            String[] parts = cleaned.split(sep);
            try {
                int s = Integer.parseInt(parts[0].trim());
                int e = Integer.parseInt(parts[1].trim());
                return (s > 0 && e > 0 && s <= e) ? new int[]{s, e} : null;
            } catch (NumberFormatException ex) { return null; }
        }
        try {
            int s = Integer.parseInt(cleaned);
            return s > 0 ? new int[]{s, s} : null;
        } catch (NumberFormatException ex) { return null; }
    }

    private Color getCourseColor(String courseId) {
        Color[] colors = {
                new Color(200, 80, 80), new Color(80, 160, 80), new Color(80, 130, 200),
                new Color(200, 160, 60), new Color(160, 80, 160), new Color(60, 180, 180),
                new Color(200, 120, 80), new Color(100, 160, 200), new Color(180, 100, 100),
                new Color(100, 180, 130)
        };
        return colors[Math.abs(courseId.hashCode()) % colors.length];
    }

    private String str(Object obj) {
        return obj == null ? "" : obj.toString();
    }
    /**
     * 教师端定时轮询：每 5 秒刷新一次课程卡片
     * 目的：管理员改完课程后，教师端能自动看到
     */
    private void startPolling() {
        if (pollTimer != null) pollTimer.stop();
        pollTimer = new javax.swing.Timer(POLL_INTERVAL_MS, e -> {
            // 只有"课程列表"视图才自动刷新，学生名单视图不刷（避免打断查看）
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    return null;
                }
                @Override
                protected void done() {
                    // 只刷新课程列表，不刷新学生名单
                    loadCoursesFromServer();
                }
            }.execute();
        });
        pollTimer.setRepeats(true);
        pollTimer.start();
    }

    @Override
    public void removeNotify() {
        if (pollTimer != null) {
            pollTimer.stop();
            pollTimer = null;
        }
        super.removeNotify();
    }
}