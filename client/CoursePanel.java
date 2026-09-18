package vCampus.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import vCampus.common.Message;
import vCampus.common.consts.MsgConst;
import vCampus.common.vo.CourseDisplayDTO;
import vCampus.common.vo.SmartSelectResultDTO;
import vCampus.common.vo.SelectResultDetail;
import vCampus.common.vo.SelectRound;
import vCampus.common.vo.Student;

public class CoursePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private Object currentUser;
    private String studentId;

    private JComboBox<String> cbConflict;
    private JComboBox<String> cbCourseNature;
    private JComboBox<String> cbCourseType;
    private JComboBox<String> cbCampus;
    private JComboBox<String> cbRound;
    private JLabel lblRoundInfo;
    private JTextField txtKeyword;
    private JCheckBox chkSmartSelect;
    private JButton btnSearch;

    private JPanel courseListPanel;
    private JScrollPane scrollPane;

    private JButton btnSubmitAll;
    private JLabel lblSelectedCount;
    private JButton btnSelectedCourses;

    private JPanel centerContainer;
    private CardLayout centerCardLayout;
    private JPanel courseListView;
    private JPanel detailView;
    private JButton btnBack;
    private JLabel lblTitle;
    private javax.swing.Timer roundTimer;
    private javax.swing.Timer pollTimer;

    private List<CourseDisplayDTO> allCourses = new ArrayList<>();
    private List<CourseDisplayDTO> submittedCourses = new ArrayList<>();
    private Map<String, Boolean> selectedMap = new HashMap<>();
    private List<SelectRound> openRounds = new ArrayList<>();
    private SelectRound currentRound;
    private List<SelectRound> allRoundsForTimer = new ArrayList<>();
    private boolean showSelectedOnly = false;

    // 模式：0=学生选课；1=重修课程；2=全校课程查询
    private int currentMode = 0;
    private JButton btnModeStudent;
    private JButton btnModeRetake;
    private JButton btnModeAll;

    // ★ 轮询用：防止轮询与提交/智能选课打架
    private volatile boolean busy = false;
    private static final int POLL_INTERVAL_MS = 5000;

    private static final Color BG_COLOR = new Color(242, 250, 253);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(216, 237, 230);
    private static final Color BTN_GREEN = new Color(183, 220, 187);
    private static final Color BTN_SELECTED = new Color(130, 180, 140);
    private static final Color HEADER_BG = new Color(235, 245, 240);
    private static final Color FULL_COLOR = new Color(255, 200, 200);
    private static final Color CONFLICT_COLOR = new Color(255, 220, 220);
    private static final Color SAME_COURSE_CONFLICT_COLOR = new Color(255, 240, 200);
    private static final Color RETAKE_COLOR = new Color(255, 240, 200);
    private static final Color RETAKE_TEXT_COLOR = new Color(200, 120, 50);
    private static final Color PENDING_TEXT_COLOR = new Color(200, 120, 50);

    private static final int CLASS_ROW_HEIGHT = 40;

    private JLabel lblTotal;

    public CoursePanel(Object user) {
        this.currentUser = user;
        if (user instanceof Student) {
            this.studentId = ((Student) user).getSCard();
        } else {
            this.studentId = "S001";
        }
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        initUI();
        loadRounds();
        loadCoursesFromServer();
        startPolling();
    }

    private void initUI() {
        add(createHeaderPanel(), BorderLayout.NORTH);

        centerCardLayout = new CardLayout();
        centerContainer = new JPanel(centerCardLayout);
        centerContainer.setBackground(BG_COLOR);

        courseListView = new JPanel(new BorderLayout());
        courseListView.setBackground(BG_COLOR);
        courseListView.setBorder(new EmptyBorder(10, 20, 10, 20));
        courseListView.add(createFilterPanel(), BorderLayout.NORTH);

        courseListPanel = new JPanel();
        courseListPanel.setLayout(new BoxLayout(courseListPanel, BoxLayout.Y_AXIS));
        courseListPanel.setBackground(CARD_BG);
        courseListPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        scrollPane = new JScrollPane(courseListPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.setBackground(CARD_BG);
        scrollPane.getViewport().setBackground(CARD_BG);
        courseListView.add(scrollPane, BorderLayout.CENTER);

        detailView = new JPanel(new BorderLayout());
        detailView.setBackground(BG_COLOR);
        detailView.setBorder(new EmptyBorder(10, 20, 10, 20));

        centerContainer.add(courseListView, "list");
        centerContainer.add(detailView, "detail");

        add(centerContainer, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }
    private boolean suppressRoundChange = false; 
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(new EmptyBorder(12, 25, 12, 25));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setBackground(HEADER_BG);

        btnBack = new JButton("<");
        btnBack.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        btnBack.setForeground(new Color(40, 60, 50));
        btnBack.setBackground(HEADER_BG);
        btnBack.setBorder(new EmptyBorder(0, 5, 0, 5));
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBack.setFocusPainted(false);
        btnBack.setContentAreaFilled(false);
        btnBack.setVisible(false);
        btnBack.addActionListener(e -> showListView());
        leftPanel.add(btnBack);

        lblTitle = new JLabel("学生选课");
        lblTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        lblTitle.setForeground(new Color(40, 60, 50));
        leftPanel.add(lblTitle);

        btnModeStudent = new JButton("学生选课");
        btnModeRetake = new JButton("重修课程");
        btnModeAll = new JButton("全校课程查询");

        for (JButton b : new JButton[]{btnModeStudent, btnModeRetake, btnModeAll}) {
            b.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            b.setBorder(new EmptyBorder(4, 12, 4, 12));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.setFocusPainted(false);
        }

        btnModeStudent.addActionListener(e -> switchMode(0));
        btnModeRetake.addActionListener(e -> switchMode(1));
        btnModeAll.addActionListener(e -> switchMode(2));

        leftPanel.add(btnModeStudent);
        leftPanel.add(btnModeRetake);
        leftPanel.add(btnModeAll);

        updateModeButtons();

        JLabel lblRoundLbl = new JLabel("选课轮次：");
        lblRoundLbl.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblRoundLbl.setForeground(new Color(100, 120, 110));
        leftPanel.add(lblRoundLbl);

        cbRound = new JComboBox<>();
        cbRound.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        cbRound.setPreferredSize(new Dimension(240, 28));
        cbRound.addActionListener(e -> {
        	if (suppressRoundChange) return;
            int idx = cbRound.getSelectedIndex();
            if (idx >= 0 && idx < openRounds.size()) {
                currentRound = openRounds.get(idx);
                updateRoundInfo();
                loadCoursesFromServer();
            }
        });
        leftPanel.add(cbRound);

        lblRoundInfo = new JLabel("");
        lblRoundInfo.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        leftPanel.add(lblRoundInfo);

        cbCampus = new JComboBox<>(new String[] { "全部校区", "九龙湖", "四牌楼", "丁家桥" });
        cbCampus.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        cbCampus.addActionListener(e -> { if (!showSelectedOnly) loadCoursesFromServer(); });
        leftPanel.add(cbCampus);

        header.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(HEADER_BG);

        JButton btnSchedule = new JButton("查看课表");
        btnSchedule.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        btnSchedule.setBackground(new Color(70, 150, 200));
        btnSchedule.setForeground(Color.WHITE);
        btnSchedule.setBorder(new EmptyBorder(6, 16, 6, 16));
        btnSchedule.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSchedule.addActionListener(e -> showScheduleDialog());
        rightPanel.add(btnSchedule);

        btnSelectedCourses = new JButton("已选课程");
        btnSelectedCourses.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        btnSelectedCourses.setBackground(new Color(100, 180, 120));
        btnSelectedCourses.setForeground(Color.WHITE);
        btnSelectedCourses.setBorder(new EmptyBorder(6, 16, 6, 16));
        btnSelectedCourses.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSelectedCourses.addActionListener(e -> toggleSelectedView());
        rightPanel.add(btnSelectedCourses);

        JLabel lblHelp = new JLabel("选课帮助");
        lblHelp.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblHelp.setForeground(new Color(80, 130, 100));
        lblHelp.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblHelp.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showHelpDialog(); }
        });
        rightPanel.add(lblHelp);

        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ============================================================
    // 模式切换
    // ============================================================
    private void switchMode(int mode) {
        currentMode = mode;
        updateModeButtons();

        if (mode == 0) lblTitle.setText("学生选课");
        else if (mode == 1) lblTitle.setText("重修课程");
        else lblTitle.setText("全校课程查询");

        if (showSelectedOnly) {
            showSelectedOnly = false;
            btnSelectedCourses.setText("已选课程");
            btnSelectedCourses.setBackground(new Color(100, 180, 120));
        }
        btnSelectedCourses.setEnabled(mode != 2);

        showListView();
        selectedMap.clear();
        loadCoursesFromServer();
    }

    private void updateModeButtons() {
        styleModeBtn(btnModeStudent, currentMode == 0);
        styleModeBtn(btnModeRetake, currentMode == 1);
        styleModeBtn(btnModeAll, currentMode == 2);
    }

    private void styleModeBtn(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(100, 160, 200));
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        } else {
            btn.setBackground(new Color(235, 242, 248));
            btn.setForeground(new Color(60, 80, 100));
            btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        }
    }

    // ============================================================
    // 视图切换
    // ============================================================
    private void showListView() {
        btnBack.setVisible(false);
        if (currentMode == 0) lblTitle.setText("学生选课");
        else if (currentMode == 1) lblTitle.setText("重修课程");
        else lblTitle.setText("全校课程查询");
        centerCardLayout.show(centerContainer, "list");
    }

    private void showDetailView(List<CourseDisplayDTO> items) {
        if (items == null || items.isEmpty()) return;
        btnBack.setVisible(true);
        lblTitle.setText("课程详情");
        detailView.removeAll();

        CourseDisplayDTO first = items.get(0);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(BG_COLOR);
        wrapper.setBorder(new EmptyBorder(15, 30, 15, 30));

        JPanel courseHeader = new JPanel(new BorderLayout());
        courseHeader.setBackground(new Color(80, 140, 100));
        courseHeader.setBorder(new EmptyBorder(25, 30, 25, 30));
        courseHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        courseHeader.setPreferredSize(new Dimension(900, 120));

        JPanel headerLeft = new JPanel();
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
        headerLeft.setBackground(new Color(80, 140, 100));

        JLabel lblName = new JLabel(first.getCourseName());
        lblName.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLeft.add(lblName);

        headerLeft.add(Box.createVerticalStrut(10));

        JLabel lblSub = new JLabel(
                first.getCourseId() + "  ·  " + str(first.getNature())
                + "  ·  " + str(first.getCourseType())
                + "  ·  " + first.getCredit() + " 学分"
                + "  ·  " + str(first.getDepartment()));
        lblSub.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        lblSub.setForeground(new Color(230, 245, 235));
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLeft.add(lblSub);

        courseHeader.add(headerLeft, BorderLayout.WEST);

        JLabel lblCount = new JLabel(items.size() + " 个教学班");
        lblCount.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        lblCount.setForeground(Color.WHITE);
        lblCount.setBorder(new EmptyBorder(6, 20, 6, 20));
        lblCount.setOpaque(true);
        lblCount.setBackground(new Color(100, 160, 120));
        JPanel countWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        countWrap.setBackground(new Color(80, 140, 100));
        countWrap.add(lblCount);
        courseHeader.add(countWrap, BorderLayout.EAST);

        wrapper.add(courseHeader);
        wrapper.add(Box.createVerticalStrut(20));

        for (CourseDisplayDTO item : items) {
            JPanel card = createOfferingCard(item);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            card.setPreferredSize(new Dimension(900, 180));
            wrapper.add(card);
            wrapper.add(Box.createVerticalStrut(12));
        }

        JScrollPane sp = new JScrollPane(wrapper);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getViewport().setBackground(BG_COLOR);
        detailView.add(sp, BorderLayout.CENTER);

        centerCardLayout.show(centerContainer, "detail");
    }

    private JPanel createOfferingCard(CourseDisplayDTO item) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(0, 0, 0, 0)));

        JPanel leftBar = new JPanel();
        leftBar.setPreferredSize(new Dimension(6, 0));
        leftBar.setBackground(getStatusColor(item));
        card.add(leftBar, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(CARD_BG);
        infoPanel.setBorder(new EmptyBorder(15, 25, 15, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 20);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblClassNum = new JLabel("教学班 " + item.getClassNum());
        lblClassNum.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        lblClassNum.setForeground(new Color(60, 100, 80));
        infoPanel.add(lblClassNum, gbc);

        gbc.gridx = 1; gbc.weightx = 0;
        JLabel lblTeacher = new JLabel("教师：" + displayOrPending(item.getTeacherName()));
        lblTeacher.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblTeacher.setForeground(isOfferingIncomplete(item) ? PENDING_TEXT_COLOR : new Color(60, 60, 60));
        infoPanel.add(lblTeacher, gbc);

        if ("retake".equals(item.getSelectType())) {
            gbc.gridx = 2; gbc.weightx = 0;
            infoPanel.add(createRetakeLabel(), gbc);
            gbc.gridx = 3; gbc.weightx = 1; gbc.anchor = GridBagConstraints.EAST;
        } else {
            gbc.gridx = 2; gbc.weightx = 1; gbc.anchor = GridBagConstraints.EAST;
        }
        JLabel statusLabel = new JLabel(formatStatus(item));
        statusLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(getStatusColor(item));
        statusLabel.setBorder(new EmptyBorder(3, 12, 3, 12));
        infoPanel.add(statusLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        gbc.weightx = 1; gbc.anchor = GridBagConstraints.WEST;
        JLabel lblTime = new JLabel("时间：" + displayOrPending(item.getSchedule()));
        lblTime.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblTime.setForeground(isOfferingIncomplete(item) ? PENDING_TEXT_COLOR : new Color(100, 100, 100));
        infoPanel.add(lblTime, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4;
        JLabel lblRoom = new JLabel("教室：" + displayOrPending(item.getClassroom())
                + "      校区：" + str(item.getCampus()));
        lblRoom.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblRoom.setForeground(isOfferingIncomplete(item) ? PENDING_TEXT_COLOR : new Color(100, 100, 100));
        infoPanel.add(lblRoom, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        int remaining = Math.max(0, item.getCapacity() - item.getEnrolledCount());
        String capText = "容量：" + item.getEnrolledCount() + " / " + item.getCapacity() + " 人"
                + "      剩余：" + remaining + " 人";
        JLabel lblCap = new JLabel(capText);
        lblCap.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblCap.setForeground(item.isFull() ? Color.RED : new Color(80, 140, 80));
        infoPanel.add(lblCap, gbc);
        card.add(infoPanel, BorderLayout.CENTER);

        if (item.isSelected()) {
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 180, 120), 2),
                    new EmptyBorder(0, 0, 0, 0)));
        }

        return card;
    }

    private Color getStatusColor(CourseDisplayDTO course) {
        if (course.isSelected()) return new Color(80, 160, 100);
        if (course.isSameCourseConflict()) return new Color(200, 150, 60);
        if (course.isConflict()) return new Color(200, 80, 80);
        if (course.isFull()) return new Color(180, 80, 80);
        return new Color(70, 130, 180);
    }

    private String formatStatus(CourseDisplayDTO course) {
        if (course.isSelected()) return "已选";
        if (course.isSameCourseConflict()) return "同课冲突";
        if (course.isConflict()) return "时间冲突";
        if (course.isFull()) return "已满";
        return "可选";
    }

    private String str(Object obj) { return obj == null ? "" : obj.toString(); }

    private String displayOrPending(String s) {
        if (s == null || s.isEmpty()) return "（待填写）";
        return s;
    }

    private boolean isOfferingIncomplete(CourseDisplayDTO item) {
        return item.getTeacherName() == null || item.getTeacherName().isEmpty()
                || item.getSchedule() == null || item.getSchedule().isEmpty()
                || item.getClassroom() == null || item.getClassroom().isEmpty();
    }

    private JLabel createRetakeLabel() {
        JLabel lbl = new JLabel("重修");
        lbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
        lbl.setForeground(RETAKE_TEXT_COLOR);
        lbl.setBackground(RETAKE_COLOR);
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(1, 8, 1, 8));
        return lbl;
    }

    // ============================================================
    // 轮次
    // ============================================================
    private void loadRounds() {
        suppressRoundChange = true;
        try {
            openRounds.clear();
            allRoundsForTimer.clear();
            cbRound.removeAllItems();

            try {
                Message request = new Message();
                request.setType(MsgConst.ADMIN_GET_ALL_ROUNDS);
                request.setData(null);
                Message response = SocketClient.send(request);

                if (response != null && response.isSuccess() && response.getData() != null) {
                    @SuppressWarnings("unchecked")
                    List<SelectRound> all = (List<SelectRound>) response.getData();
                    allRoundsForTimer.addAll(all);

                    for (SelectRound r : all) {
                        if ("open".equals(r.getStatus()) || "upcoming".equals(r.getStatus())) {
                            openRounds.add(r);
                        }
                    }

                    openRounds.sort((a, b) -> {
                        boolean aOpen = "open".equals(a.getStatus());
                        boolean bOpen = "open".equals(b.getStatus());
                        if (aOpen != bOpen) return aOpen ? -1 : 1;
                        String sa = a.getStartTime() != null ? a.getStartTime() : "";
                        String sb = b.getStartTime() != null ? b.getStartTime() : "";
                        return sa.compareTo(sb);
                    });

                    if (openRounds.isEmpty()) {
                        cbRound.addItem("当前无可选轮次");
                        cbRound.setEnabled(false);
                        currentRound = null;
                        lblRoundInfo.setText("（无开放轮次）");
                        lblRoundInfo.setForeground(new Color(200, 80, 80));
                    } else {
                        for (SelectRound r : openRounds) cbRound.addItem(formatRoundDisplay(r));
                        cbRound.setEnabled(true);
                        cbRound.setSelectedIndex(0);
                        currentRound = openRounds.get(0);
                        updateRoundInfo();
                    }
                } else {
                    cbRound.addItem("加载失败");
                    cbRound.setEnabled(false);
                    currentRound = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                cbRound.addItem("连接失败");
                cbRound.setEnabled(false);
                currentRound = null;
            }
        } finally {
            suppressRoundChange = false;
        }

        scheduleNextRoundRefresh();
        loadCoursesFromServer();
    }

    private String formatRoundDisplay(SelectRound r) {
        if (r == null) return "";
        String name = r.getRoundName() != null ? r.getRoundName() : r.getRoundId();
        if ("open".equals(r.getStatus())) {
            return name + " [开放中]";
        } else if ("upcoming".equals(r.getStatus())) {
            String st = r.getStartTime();
            if (st != null && st.length() > 16) st = st.substring(0, 16);
            return name + " [未开始 " + st + "]";
        }
        return name;
    }

    private void updateRoundInfo() {
        if (currentRound == null) { lblRoundInfo.setText(""); return; }
        String status = currentRound.getStatus();
        if ("open".equals(status)) {
            lblRoundInfo.setText("开放中 " + currentRound.getStartTime() + " ~ " + currentRound.getEndTime());
            lblRoundInfo.setForeground(new Color(60, 150, 80));
        } else if ("upcoming".equals(status)) {
            lblRoundInfo.setText("预览模式（未开始，开始时间 " + currentRound.getStartTime() + "）");
            lblRoundInfo.setForeground(new Color(200, 130, 50));
        } else {
            lblRoundInfo.setText("已结束");
            lblRoundInfo.setForeground(new Color(180, 80, 80));
        }
    }

    // ============================================================
    // 课表
    // ============================================================
    private void showScheduleDialog() {
        List<CourseDisplayDTO> selectedCourses = new ArrayList<>();
        for (CourseDisplayDTO dto : allCourses) {
            if (dto.isSelected()) selectedCourses.add(dto);
        }
        if (selectedCourses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "您还没有选课，课表为空。", "提示", JOptionPane.INFORMATION_MESSAGE);
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
                gbc.gridx = col; gbc.gridy = row;
                schedulePanel.add(cell, gbc);
                cells[row][col] = cell;
            }
        }

        Map<String, List<int[]>> courseCellMap = new LinkedHashMap<>();
        for (CourseDisplayDTO course : selectedCourses) {
            int[] p = parseSchedulePos(course.getSchedule());
            if (p == null) continue;
            String key = course.getOfferingId();
            courseCellMap.putIfAbsent(key, new ArrayList<>());
            for (int i = p[2]; i <= p[3] && i <= 10; i++) {
                courseCellMap.get(key).add(new int[]{i, p[1]});
            }
        }

        for (CourseDisplayDTO course : selectedCourses) {
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
                    ? BorderFactory.createLineBorder(new Color(200,200,200), 1)
                    : BorderFactory.createMatteBorder(1, 1, 0, 1, new Color(200,200,200)));
            if (cellPos.size() > 1) {
                cells[last[0]][last[1]].setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, new Color(200,200,200)));
            }
            for (int i = 1; i < cellPos.size() - 1; i++) {
                int[] p = cellPos.get(i);
                cells[p[0]][p[1]].setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, new Color(200,200,200)));
            }
        }

        JScrollPane sp = new JScrollPane(schedulePanel);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(10, 20, 10, 20));
        JLabel info = new JLabel("共 " + selectedCourses.size() + " 门课程");
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

    private int[] parseSchedulePos(String schedule) {
        if (schedule == null || schedule.isEmpty()) return null;
        String dayStr = null, periodStr = null;

        if (schedule.contains(" ")) {
            String[] parts = schedule.split("\\s+");
            if (parts.length >= 2) { dayStr = parts[0]; periodStr = parts[1]; }
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

    // ============================================================
    // 帮助
    // ============================================================
    private void showHelpDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "选课帮助", true);
        d.setSize(560, 500);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.setBackground(Color.WHITE);

        String[] steps = {
                "三种模式说明",
                "",
                "【学生选课】",
                "   - 只显示你培养方案内的课程",
                "   - 按推荐学期匹配当前学期",
                "   - 这是你正常选课的入口",
                "",
                "【重修课程】",
                "   - 显示你培养方案外、但过去挂科、且本轮有开课的课程",
                "   - 重修课无视时间冲突",
                "   - 勾选后与普通选课一起提交",
                "",
                "【全校课程查询】",
                "   - 显示本轮次所有开课班级",
                "   - 仅用于查看，不能勾选",
                "",
                "提交与退课",
                "   - 在【学生选课】或【重修课程】下勾选，点击一键提交",
                "   - 点击右上角已选课程切换到已选视图，可退课"
        };

        for (String line : steps) {
            JLabel lbl = new JLabel(line.isEmpty() ? " " : line);
            if (line.startsWith("   ")) {
                lbl.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
                lbl.setForeground(new Color(80, 80, 80));
            } else if (line.startsWith("【")) {
                lbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
                lbl.setForeground(new Color(60, 100, 140));
            } else {
                lbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
                lbl.setForeground(new Color(40, 60, 50));
            }
            content.add(lbl);
            content.add(Box.createVerticalStrut(4));
        }

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        d.add(sp, BorderLayout.CENTER);

        JButton close = new JButton("我知道了");
        close.setBackground(new Color(100, 180, 120));
        close.setForeground(Color.WHITE);
        close.setBorder(new EmptyBorder(8, 30, 8, 30));
        close.addActionListener(e -> d.dispose());
        JPanel bp = new JPanel();
        bp.setBackground(Color.WHITE);
        bp.add(close);
        d.add(bp, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ============================================================
    // 已选视图切换
    // ============================================================
    private void toggleSelectedView() {
        if (currentMode == 2) {
            JOptionPane.showMessageDialog(this, "全校课程查询模式下无法查看已选课程",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showSelectedOnly = !showSelectedOnly;
        if (showSelectedOnly) {
            btnSelectedCourses.setText("全部课程");
            btnSelectedCourses.setBackground(new Color(220, 180, 100));
        } else {
            btnSelectedCourses.setText("已选课程");
            btnSelectedCourses.setBackground(new Color(100, 180, 120));
        }
        showListView();
        refreshCurrentView();
    }

    // ============================================================
    // 筛选
    // ============================================================
    private JPanel createFilterPanel() {
        JPanel filter = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 6));
        filter.setBackground(CARD_BG);
        filter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 15, 8, 15)));

        filter.add(createLabel("是否冲突："));
        cbConflict = new JComboBox<>(new String[] { "全部", "是", "否" });
        cbConflict.setPreferredSize(new Dimension(90, 28));
        cbConflict.addActionListener(e -> applyFilters());
        filter.add(cbConflict);

        filter.add(createLabel("课程性质："));
        cbCourseNature = new JComboBox<>(new String[] { "全部", "必修", "限选", "任选", "重修" });
        cbCourseNature.setPreferredSize(new Dimension(90, 28));
        cbCourseNature.addActionListener(e -> applyFilters());
        filter.add(cbCourseNature);

        filter.add(createLabel("课程类别："));
        cbCourseType = new JComboBox<>(new String[] { "全部", "思政类", "军体类", "外语类", "计算机类",
                "通识选修课程", "新生研讨课", "大类学科基础课", "专业方向及跨学科选修课",
                "导论类", "集中实践环节（含课外实践）&短学期课程", "专业主干课", "通识教育基础课", "专业相关课程" });
        cbCourseType.setPreferredSize(new Dimension(90, 28));
        cbCourseType.addActionListener(e -> applyFilters());
        filter.add(cbCourseType);

        filter.add(createLabel("关键词："));
        txtKeyword = new JTextField(12);
        txtKeyword.setPreferredSize(new Dimension(140, 28));
        txtKeyword.addActionListener(e -> searchCourses());
        filter.add(txtKeyword);

        btnSearch = new JButton("搜索");
        btnSearch.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        btnSearch.setBackground(BTN_GREEN);
        btnSearch.setBorder(new EmptyBorder(4, 16, 4, 16));
        btnSearch.addActionListener(e -> searchCourses());
        filter.add(btnSearch);

        chkSmartSelect = new JCheckBox("智能选课");
        chkSmartSelect.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        chkSmartSelect.setForeground(new Color(40, 80, 60));
        chkSmartSelect.setBackground(CARD_BG);
        chkSmartSelect.addActionListener(e -> {
            if (chkSmartSelect.isSelected()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "智能选课将根据您的培养方案和已修课程，自动勾选推荐的课程。\n" +
                        "提交后系统会自动刷新并继续勾选新的可选课程，适合持续抢课。",
                        "智能选课", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) smartSelectCourses();
                else chkSmartSelect.setSelected(false);
            }
        });
        filter.add(chkSmartSelect);

        return filter;
    }

    private void applyFilters() {
        // ★ 不再一空就 return，空列表也要渲染"暂无课程"
        List<CourseDisplayDTO> filtered = new ArrayList<>(allCourses);

        if (showSelectedOnly) {
            filtered.removeIf(c -> !c.isSelected());
        } else {
            String conflictFilter = (String) cbConflict.getSelectedItem();
            if (!"全部".equals(conflictFilter)) {
                boolean showConflict = "是".equals(conflictFilter);
                filtered.removeIf(c -> {
                    if (c.isSelected()) return true;
                    boolean hasConflict = c.isConflict() || c.isSameCourseConflict();
                    return hasConflict != showConflict;
                });
            }
        }

        String natureFilter = (String) cbCourseNature.getSelectedItem();
        if (!"全部".equals(natureFilter)) filtered.removeIf(c -> !natureFilter.equals(c.getNature()));

        String typeFilter = (String) cbCourseType.getSelectedItem();
        if (!"全部".equals(typeFilter)) filtered.removeIf(c -> !typeFilter.equals(c.getCourseType()));

        renderCourseList(filtered);
    }

    private static class WrapLayout extends FlowLayout {
        private static final long serialVersionUID = 1L;
        public WrapLayout() { super(); }
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
        @Override public Dimension minimumLayoutSize(Container target) { return layoutSize(target, false); }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;
                for (Component m : target.getComponents()) {
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth = 0; rowHeight = 0;
                    }
                    rowWidth += d.width + hgap;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                dim.width = Math.max(dim.width, rowWidth);
                dim.height += rowHeight;
                dim.width += insets.left + insets.right + hgap * 2;
                dim.height += insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        label.setForeground(new Color(80, 80, 80));
        return label;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(CARD_BG);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(12, 25, 12, 25)));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setBackground(CARD_BG);

        lblSelectedCount = new JLabel("已勾选 0 门课");
        lblSelectedCount.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        lblSelectedCount.setForeground(new Color(60, 60, 60));
        leftPanel.add(lblSelectedCount);

        btnSubmitAll = new JButton("一键提交");
        btnSubmitAll.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        btnSubmitAll.setBackground(new Color(100, 180, 120));
        btnSubmitAll.setForeground(Color.WHITE);
        btnSubmitAll.setBorder(new EmptyBorder(8, 30, 8, 30));
        btnSubmitAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmitAll.addActionListener(e -> submitAllSelected());
        leftPanel.add(btnSubmitAll);
        bottom.add(leftPanel, BorderLayout.WEST);

        lblTotal = new JLabel("共 0 门课可选");
        lblTotal.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        lblTotal.setForeground(new Color(150, 150, 150));
        bottom.add(lblTotal, BorderLayout.EAST);
        return bottom;
    }

    // ============================================================
    // 加载课程（拆分：拉数据 / 渲染）
    // ============================================================

    /** 拉数据 + 渲染（EDT 里调） */
    private void loadCoursesFromServer() {
        fetchCoursesFromServer();
        updateTotalCount();
        refreshCurrentView();
        updateBottomStats();
    }

    /** ★ 只拉数据，不碰 UI（后台线程可调） */
    private void fetchCoursesFromServer() {
        if (currentRound == null) {
            allCourses.clear();
            return;
        }
        String st = currentRound.getStatus();
        if (!"open".equals(st) && !"upcoming".equals(st)) {
            allCourses.clear();
            return;
        }
        try {
            String campus = (String) cbCampus.getSelectedItem();
            if (campus == null) campus = "全部校区";

            Map<String, Object> params = new HashMap<>();
            params.put("studentId", studentId);
            params.put("campus", campus);
            params.put("roundId", currentRound.getRoundId());
            params.put("semester", currentRound.getSemester());

            Message request = new Message();
            if (currentMode == 2) {
                request.setType(MsgConst.GET_ALL_COURSES_BY_ROUND);
            } else if (currentMode == 1) {
                request.setType(MsgConst.GET_RETAKE_COURSES_BY_ROUND);
            } else {
                request.setType(MsgConst.GET_AVAILABLE_COURSES);
            }
            request.setData(params);
            Message response = SocketClient.send(request);

            if (response != null && response.isSuccess() && response.getData() != null) {
                @SuppressWarnings("unchecked")
                List<CourseDisplayDTO> courses = (List<CourseDisplayDTO>) response.getData();
                allCourses = courses;
            } else {
                allCourses.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            allCourses.clear();
        }
    }

    private void renderPendingRoundCard(SelectRound round) {
        courseListPanel.removeAll();

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(40, 40, 40, 40)));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        if (round == null) {
            JLabel lbl = new JLabel("当前没有可用的选课轮次", SwingConstants.CENTER);
            lbl.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
            lbl.setForeground(new Color(120, 120, 120));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lbl);
        } else {
            JLabel lblTitle = new JLabel("选课尚未开始", SwingConstants.CENTER);
            lblTitle.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
            lblTitle.setForeground(new Color(200, 130, 50));
            lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblTitle);

            card.add(Box.createVerticalStrut(20));

            JLabel lblRound = new JLabel("轮次：" + round.getRoundName(), SwingConstants.CENTER);
            lblRound.setFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
            lblRound.setForeground(new Color(60, 60, 60));
            lblRound.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblRound);

            card.add(Box.createVerticalStrut(10));

            JLabel lblStart = new JLabel("开始时间：" + round.getStartTime(), SwingConstants.CENTER);
            lblStart.setFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
            lblStart.setForeground(new Color(60, 130, 80));
            lblStart.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblStart);

            card.add(Box.createVerticalStrut(10));

            JLabel lblEnd = new JLabel("结束时间：" + round.getEndTime(), SwingConstants.CENTER);
            lblEnd.setFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
            lblEnd.setForeground(new Color(120, 120, 120));
            lblEnd.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblEnd);

            card.add(Box.createVerticalStrut(20));

            JLabel lblHint = new JLabel("到时间后会自动刷新，无需手动操作", SwingConstants.CENTER);
            lblHint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            lblHint.setForeground(new Color(150, 150, 150));
            lblHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblHint);
        }

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(CARD_BG);
        wrapper.add(card);

        courseListPanel.add(Box.createVerticalStrut(30));
        courseListPanel.add(wrapper);

        courseListPanel.revalidate();
        courseListPanel.repaint();

        lblTotal.setText("共 0 门课可选");
        lblSelectedCount.setText("已勾选 0 门课");
        btnSubmitAll.setEnabled(false);
        btnSubmitAll.setText("一键提交");
    }

    /**
     * ★ 修复：轮次无效时正确显示提示卡片；否则按当前视图渲染
     */
    private void refreshCurrentView() {
        if (currentRound == null
                || (!"open".equals(currentRound.getStatus())
                    && !"upcoming".equals(currentRound.getStatus()))) {
            renderPendingRoundCard(currentRound);
            return;
        }
        if (showSelectedOnly) {
            List<CourseDisplayDTO> selected = new ArrayList<>();
            for (CourseDisplayDTO dto : allCourses) {
                if (dto.isSelected()) selected.add(dto);
            }
            submittedCourses = selected;
            renderCourseList(submittedCourses);
            return;
        }
        // ★ 如果搜索框里有内容，保留搜索结果，不覆盖
        String kw = (txtKeyword != null) ? txtKeyword.getText().trim() : "";
        if (!kw.isEmpty()) {
            searchCourses();
            return;
        }
        applyFilters();
    }

    // ============================================================
    // 提交选课
    // ============================================================
    private void submitAllSelected() {
        if (currentMode == 2) {
            JOptionPane.showMessageDialog(this, "全校课程查询模式下无法提交选课",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentRound == null || !"open".equals(currentRound.getStatus())) {
            JOptionPane.showMessageDialog(this, "当前轮次未开放，无法提交选课",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> selectedIds = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectedMap.entrySet()) {
            if (entry.getValue()) selectedIds.add(entry.getKey());
        }

        if (selectedIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "您还没有勾选任何课程，请先勾选课程后再提交。",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "您确定要提交 " + selectedIds.size() + " 门课的选课请求吗？",
                "确认提交", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        busy = true;   // ★ 防止轮询打断
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("studentId", studentId);
            params.put("offeringIds", selectedIds);
            params.put("semester", currentRound.getSemester());
            params.put("roundId", currentRound.getRoundId());

            Message request = new Message();
            request.setType(MsgConst.SUBMIT_ALL_COURSES);
            request.setData(params);
            Message response = SocketClient.send(request);

            if (response != null && response.isSuccess() && response.getData() != null) {
            	SmartSelectResultDTO result = (SmartSelectResultDTO) response.getData();
            	StringBuilder msg = new StringBuilder("<html><b>选课结果：</b><br><br>");
            	for (SelectResultDetail detail : result.getDetails()) {
            	    String color = detail.isSuccess() ? "#2E7D32" : "#C62828";
            	    String mark = detail.isSuccess() ? "成功" : "失败";
            	    msg.append("<span style='color:").append(color).append("'>")
            	       .append(mark).append(" ")
            	       .append(detail.getCourseName())
            	       .append(" —— ").append(detail.getMessage())
            	       .append("</span><br>");
            	}
            	msg.append("<br><b>").append(result.getMessage()).append("</b></html>");
            	JOptionPane.showMessageDialog(this, msg.toString(), "提交结果", JOptionPane.INFORMATION_MESSAGE);

                // ★ 只清成功的，失败的保留勾选
                Set<String> successIds = new HashSet<>();
                for (SelectResultDetail d : result.getDetails()) {
                    if (d.isSuccess()) successIds.add(d.getOfferingId());
                }
                selectedMap.keySet().removeAll(successIds);

                loadCoursesFromServer();
                if (chkSmartSelect.isSelected()) smartSelectCourses();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            busy = false;
        }
    }

    // ============================================================
    // 退课
    // ============================================================
    private void dropCourse(String offeringId, String courseName) {
        if (currentRound == null || !"open".equals(currentRound.getStatus())) {
            JOptionPane.showMessageDialog(this, "当前轮次未开放，无法退课",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要退选 " + courseName + " 吗？",
                "确认退选", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        busy = true;
        try {
            Map<String, String> params = new HashMap<>();
            params.put("studentId", studentId);
            params.put("offeringId", offeringId);

            Message request = new Message();
            request.setType(MsgConst.DROP_COURSE);
            request.setData(params);
            Message response = SocketClient.send(request);

            if (response != null && response.isSuccess()) {
                JOptionPane.showMessageDialog(this, "退课成功", "提示", JOptionPane.INFORMATION_MESSAGE);
                loadCoursesFromServer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            busy = false;
        }
    }

    // ============================================================
    // 智能选课
    // ============================================================
    private void smartSelectCourses() {
        if (currentMode == 2) {
            JOptionPane.showMessageDialog(this, "全校课程查询模式下无法使用智能选课",
                    "提示", JOptionPane.WARNING_MESSAGE);
            chkSmartSelect.setSelected(false);
            return;
        }
        if (currentRound == null || !"open".equals(currentRound.getStatus())) {
            JOptionPane.showMessageDialog(this, "当前轮次未开放，无法使用智能选课",
                    "提示", JOptionPane.WARNING_MESSAGE);
            chkSmartSelect.setSelected(false);
            return;
        }

        busy = true;
        try {
            selectedMap.clear();
            int count = 0;

            Set<String> selectedCourseIds = new HashSet<>();
            Set<String> pendingCourseIds = new HashSet<>();
            List<String> pendingSchedules = new ArrayList<>();

            for (CourseDisplayDTO dto : allCourses) {
                if (dto.isSelected()) {
                    selectedCourseIds.add(dto.getCourseId());
                    if (!"retake".equals(dto.getSelectType())
                            && dto.getSchedule() != null && !dto.getSchedule().isEmpty()) {
                        pendingSchedules.add(dto.getSchedule());
                    }
                }
            }

            List<CourseDisplayDTO> requiredCourses = new ArrayList<>();
            List<CourseDisplayDTO> limitedCourses = new ArrayList<>();
            List<CourseDisplayDTO> electiveCourses = new ArrayList<>();

            for (CourseDisplayDTO dto : allCourses) {
                if (dto.isFull() || dto.isSelected()) continue;
                if (selectedCourseIds.contains(dto.getCourseId())) continue;
                if (dto.getSchedule() == null || dto.getSchedule().isEmpty()) continue;

                String nature = dto.getNature();
                if (nature == null) nature = "";

                if ("必修".equals(nature) || "required".equalsIgnoreCase(nature)) {
                    requiredCourses.add(dto);
                } else if ("限选".equals(nature) || "limited".equalsIgnoreCase(nature)) {
                    limitedCourses.add(dto);
                } else {
                    electiveCourses.add(dto);
                }
            }

            List<CourseDisplayDTO> prioritized = new ArrayList<>();
            prioritized.addAll(requiredCourses);
            prioritized.addAll(limitedCourses);
            prioritized.addAll(electiveCourses);

            int maxSelect = 10;
            int selectedCount = 0;

            for (CourseDisplayDTO dto : prioritized) {
                if (selectedCount >= maxSelect) break;
                if (pendingCourseIds.contains(dto.getCourseId())) continue;
                if (dto.isSameCourseConflict()) continue;

                String schedule = dto.getSchedule();
                boolean isRetake = "retake".equals(dto.getSelectType());

                if (!isRetake) {
                    boolean conflictWithExisting = false;
                    for (String ps : pendingSchedules) {
                        if (isScheduleConflict(schedule, ps)) {
                            conflictWithExisting = true;
                            break;
                        }
                    }
                    if (conflictWithExisting) continue;
                }

                selectedMap.put(dto.getOfferingId(), true);
                pendingCourseIds.add(dto.getCourseId());

                if (!isRetake) {
                    pendingSchedules.add(schedule);
                }
                selectedCount++;
                count++;
            }

            renderCourseList(allCourses);

            String msg = "已自动勾选 " + count + " 门推荐课程。\n"
                    + "优先顺序：必修、限选、任选；已自动避开时间冲突（重修课程除外）。\n"
                    + "提交后系统会自动刷新并继续勾选新的可选课程。";
            JOptionPane.showMessageDialog(this, msg, "智能选课", JOptionPane.INFORMATION_MESSAGE);
            updateBottomStats();
        } finally {
            busy = false;
        }
    }

    private boolean isScheduleConflict(String s1, String s2) {
        int[] p1 = parseSchedulePos(s1);
        int[] p2 = parseSchedulePos(s2);
        if (p1 == null || p2 == null) {
            return s1 != null && s1.trim().equals(s2 == null ? null : s2.trim());
        }
        if (p1[0] != p2[0]) return false;
        return p1[2] <= p2[3] && p2[2] <= p1[3];
    }

    // ============================================================
    // 搜索
    // ============================================================
    private void searchCourses() {
        String keyword = txtKeyword.getText().trim().toLowerCase();
        if (keyword.isEmpty()) { applyFilters(); return; }

        List<CourseDisplayDTO> filtered = new ArrayList<>();
        for (CourseDisplayDTO dto : allCourses) {
            if (dto.getCourseName().toLowerCase().contains(keyword)
                    || dto.getCourseId().toLowerCase().contains(keyword)
                    || (dto.getTeacherName() != null && dto.getTeacherName().toLowerCase().contains(keyword))) {
                filtered.add(dto);
            }
        }
        renderCourseList(filtered);
    }

    // ============================================================
    // 渲染
    // ============================================================
    private void renderCourseList(List<CourseDisplayDTO> items) {
        courseListPanel.removeAll();

        if (items == null || items.isEmpty()) {
            String msg;
            if (showSelectedOnly) {
                msg = "暂无已选课程";
            } else if (currentMode == 1) {
                msg = "该轮次并无重修课程";
            } else if (currentMode == 2) {
                msg = "暂无课程数据";
            } else {
                msg = "暂无课程数据";
            }

            JLabel emptyLabel = new JLabel(msg, SwingConstants.CENTER);
            if (currentMode == 1 && !showSelectedOnly) {
                emptyLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
                emptyLabel.setForeground(new Color(200, 130, 50));
            } else {
                emptyLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
                emptyLabel.setForeground(new Color(150, 150, 150));
            }
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            courseListPanel.add(Box.createVerticalStrut(40));
            courseListPanel.add(emptyLabel);
            courseListPanel.revalidate();
            courseListPanel.repaint();
            updateBottomStats();
            return;
        }

        if (currentRound != null && "upcoming".equals(currentRound.getStatus()) && currentMode != 2) {
            JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT));
            banner.setBackground(new Color(255, 245, 225));
            banner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 200, 150)),
                    new EmptyBorder(8, 15, 8, 15)));
            banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            banner.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblBanner = new JLabel("当前处于预览模式，开始时间 "
                    + currentRound.getStartTime() + " 后可以选课");
            lblBanner.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            lblBanner.setForeground(new Color(180, 100, 40));
            banner.add(lblBanner);

            courseListPanel.add(banner);
            courseListPanel.add(Box.createVerticalStrut(8));
        }

        if (currentMode == 2) {
            JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT));
            banner.setBackground(new Color(240, 245, 250));
            banner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 215, 230)),
                    new EmptyBorder(8, 15, 8, 15)));
            banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            banner.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblBanner = new JLabel("全校课程查询模式：仅用于查看课程信息，不能选课");
            lblBanner.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            lblBanner.setForeground(new Color(80, 110, 140));
            banner.add(lblBanner);

            courseListPanel.add(banner);
            courseListPanel.add(Box.createVerticalStrut(8));
        }

        Map<String, List<CourseDisplayDTO>> grouped = new LinkedHashMap<>();
        for (CourseDisplayDTO item : items) {
            grouped.computeIfAbsent(item.getCourseId(), k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<CourseDisplayDTO>> entry : grouped.entrySet()) {
            JPanel card = createCourseCard(entry.getKey(), entry.getValue());
            courseListPanel.add(card);
            courseListPanel.add(Box.createVerticalStrut(12));
        }

        courseListPanel.revalidate();
        courseListPanel.repaint();
        updateBottomStats();
    }

    private JPanel createCourseCard(String courseId, List<CourseDisplayDTO> items) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        int cardHeight = calcCourseCardHeight(items.size());
        card.setPreferredSize(new Dimension(780, cardHeight));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, cardHeight));
        card.setMinimumSize(new Dimension(780, cardHeight));

        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(new Color(248, 252, 250));
        header.setBorder(new EmptyBorder(8, 15, 8, 15));

        CourseDisplayDTO first = items.get(0);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.weightx = 0;
        JLabel lblCourseId = new JLabel(first.getCourseId());
        lblCourseId.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        lblCourseId.setForeground(new Color(60, 100, 80));
        header.add(lblCourseId, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        JLabel lblCourseName = new JLabel(first.getCourseName());
        lblCourseName.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        lblCourseName.setForeground(new Color(40, 40, 40));
        lblCourseName.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblCourseName.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showDetailView(items); }
            public void mouseEntered(MouseEvent e) { lblCourseName.setForeground(new Color(60, 130, 90)); }
            public void mouseExited(MouseEvent e) { lblCourseName.setForeground(new Color(40, 40, 40)); }
        });
        header.add(lblCourseName, gbc);

        boolean hasRetake = false;
        for (CourseDisplayDTO dto : items) {
            if ("retake".equals(dto.getSelectType())) { hasRetake = true; break; }
        }
        gbc.gridx = 2; gbc.weightx = 0;
        if (hasRetake) {
            header.add(createRetakeLabel(), gbc);
        }

        gbc.gridx = 3; gbc.weightx = 0;
        JLabel lblNature = new JLabel(first.getNature());
        lblNature.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        lblNature.setForeground(new Color(100, 150, 120));
        lblNature.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 210, 190), 1),
                new EmptyBorder(2, 10, 2, 10)));
        header.add(lblNature, gbc);

        gbc.gridx = 4;
        JLabel lblDept = new JLabel(first.getDepartment());
        lblDept.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        lblDept.setForeground(new Color(120, 120, 120));
        header.add(lblDept, gbc);

        gbc.gridx = 5;
        JLabel lblCredit = new JLabel(first.getCredit() + " 学分");
        lblCredit.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        lblCredit.setForeground(new Color(200, 150, 50));
        header.add(lblCredit, gbc);

        gbc.gridx = 6; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        JLabel lblClassCount = new JLabel(items.size() + " 个教学班");
        lblClassCount.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        lblClassCount.setForeground(new Color(150, 150, 150));
        header.add(lblClassCount, gbc);

        card.add(header, BorderLayout.NORTH);

        JPanel classList = new JPanel();
        classList.setLayout(new BoxLayout(classList, BoxLayout.Y_AXIS));
        classList.setBackground(CARD_BG);
        classList.setBorder(new EmptyBorder(2, 15, 8, 15));

        for (CourseDisplayDTO item : items) {
            JPanel row = createClassRow(item);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, CLASS_ROW_HEIGHT));
            row.setPreferredSize(new Dimension(750, CLASS_ROW_HEIGHT));
            classList.add(row);
            classList.add(Box.createVerticalStrut(2));
        }

        card.add(classList, BorderLayout.CENTER);
        return card;
    }

    private JPanel createClassRow(CourseDisplayDTO item) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(CARD_BG);

        Color borderColor = new Color(200, 225, 210);
        if (item.isSameCourseConflict()) borderColor = new Color(255, 200, 150);
        else if (item.isConflict()) borderColor = new Color(255, 150, 150);

        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, borderColor),
                new EmptyBorder(4, 12, 4, 12)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 4, 1, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int col = 0;
        gbc.gridx = col++; gbc.weightx = 0;
        JLabel lblClassNum = new JLabel("[" + item.getClassNum() + "]");
        lblClassNum.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        lblClassNum.setForeground(new Color(80, 120, 100));
        row.add(lblClassNum, gbc);

        if ("retake".equals(item.getSelectType())) {
            gbc.gridx = col++; gbc.weightx = 0;
            row.add(createRetakeLabel(), gbc);
        }

        boolean incomplete = isOfferingIncomplete(item);

        gbc.gridx = col++; gbc.weightx = 0;
        JLabel lblTeacher = new JLabel(displayOrPending(item.getTeacherName()));
        lblTeacher.setForeground(incomplete ? PENDING_TEXT_COLOR : new Color(60, 60, 60));
        row.add(lblTeacher, gbc);

        gbc.gridx = col++; gbc.weightx = 0;
        JLabel lblTime = new JLabel(displayOrPending(item.getSchedule()));
        lblTime.setForeground(incomplete ? PENDING_TEXT_COLOR : new Color(100, 100, 100));
        row.add(lblTime, gbc);

        gbc.gridx = col++; gbc.weightx = 0;
        JLabel lblRoom = new JLabel(displayOrPending(item.getClassroom()));
        lblRoom.setForeground(incomplete ? PENDING_TEXT_COLOR : new Color(100, 100, 100));
        row.add(lblRoom, gbc);

        gbc.gridx = col++; gbc.weightx = 0;
        String capText = item.getEnrolledCount() + "/" + item.getCapacity();
        JLabel lblCap = new JLabel(capText + " 人");
        lblCap.setForeground(item.isFull() ? Color.RED : new Color(80, 160, 80));
        row.add(lblCap, gbc);

        gbc.gridx = col++; gbc.weightx = 0;
        if (item.isSelected()) {
            row.add(statusLabel("已选", new Color(40, 140, 80), new Color(200, 235, 210)), gbc);
        } else if (item.isSameCourseConflict()) {
            row.add(statusLabel("同课冲突", new Color(200, 120, 50), SAME_COURSE_CONFLICT_COLOR), gbc);
        } else if (item.isConflict()) {
            row.add(statusLabel("时间冲突", new Color(200, 50, 50), CONFLICT_COLOR), gbc);
        } else if (item.isFull()) {
            row.add(statusLabel("已满", Color.RED, FULL_COLOR), gbc);
        }

        gbc.gridx = col++; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.EAST;

        if (currentMode == 2) {
            JLabel lblView = new JLabel("仅查看");
            lblView.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            lblView.setForeground(new Color(150, 150, 150));
            row.add(lblView, gbc);
        } else if (showSelectedOnly) {
            JButton btn = new JButton("退选");
            btn.setBackground(new Color(220, 120, 120));
            btn.setForeground(Color.WHITE);
            btn.setBorder(new EmptyBorder(4, 16, 4, 16));
            btn.addActionListener(e -> dropCourse(item.getOfferingId(), item.getCourseName()));
            row.add(btn, gbc);
        } else {
            JButton btn = new JButton("选课");
            btn.setBorder(new EmptyBorder(4, 16, 4, 16));

            boolean isUpcoming = currentRound != null && "upcoming".equals(currentRound.getStatus());
            boolean disabled = isUpcoming || item.isSelected() || item.isFull()
                    || item.isConflict() || item.isSameCourseConflict();

            if (disabled) {
                if (isUpcoming) btn.setText("未开始");
                else if (item.isSelected()) btn.setText("已选");
                else if (item.isFull()) btn.setText("已满");
                else btn.setText("冲突");
                btn.setBackground(new Color(220, 220, 220));
                btn.setForeground(new Color(150, 150, 150));
                btn.setEnabled(false);
            } else {
                boolean pre = selectedMap.getOrDefault(item.getOfferingId(), false);
                boolean isRetake = "retake".equals(item.getSelectType());

                if (isRetake) {
                    btn.setText(pre ? "已勾选" : "重修选课");
                    btn.setBackground(pre ? new Color(180, 120, 60) : new Color(240, 200, 150));
                    btn.setForeground(pre ? Color.WHITE : new Color(120, 60, 20));
                } else {
                    btn.setText(pre ? "已勾选" : "选课");
                    btn.setBackground(pre ? BTN_SELECTED : BTN_GREEN);
                    btn.setForeground(pre ? Color.WHITE : new Color(40, 70, 40));
                }

                final String offeringId = item.getOfferingId();
                btn.addActionListener(e -> {
                    boolean cur = selectedMap.getOrDefault(offeringId, false);
                    selectedMap.put(offeringId, !cur);
                    if (isRetake) {
                        btn.setText(!cur ? "已勾选" : "重修选课");
                        btn.setBackground(!cur ? new Color(180, 120, 60) : new Color(240, 200, 150));
                        btn.setForeground(!cur ? Color.WHITE : new Color(120, 60, 20));
                    } else {
                        btn.setText(!cur ? "已勾选" : "选课");
                        btn.setBackground(!cur ? BTN_SELECTED : BTN_GREEN);
                        btn.setForeground(!cur ? Color.WHITE : new Color(40, 70, 40));
                    }
                    updateBottomStats();
                });
            }
            row.add(btn, gbc);
        }

        return row;
    }

    private JLabel statusLabel(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
        lbl.setForeground(fg);
        lbl.setBackground(bg);
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(1, 8, 1, 8));
        return lbl;
    }

    private void updateBottomStats() {
        int count = (int) selectedMap.values().stream().filter(Boolean::booleanValue).count();
        lblSelectedCount.setText("已勾选 " + count + " 门课");
        boolean canSubmit = currentRound != null && "open".equals(currentRound.getStatus()) && currentMode != 2;
        btnSubmitAll.setEnabled(canSubmit && count > 0);
        btnSubmitAll.setText(count > 0 ? "一键提交 (" + count + ")" : "一键提交");
    }

    private void updateTotalCount() {
        if (lblTotal != null) {
            if (currentMode == 1) lblTotal.setText("共 " + allCourses.size() + " 门重修课程");
            else if (currentMode == 2) lblTotal.setText("共 " + allCourses.size() + " 门课程");
            else lblTotal.setText("共 " + allCourses.size() + " 门课可选");
        }
    }

    private void scheduleNextRoundRefresh() {
        if (roundTimer != null) {
            roundTimer.stop();
            roundTimer = null;
        }

        long delayMillis = 10_000;

        if (allRoundsForTimer != null && !allRoundsForTimer.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                long now = System.currentTimeMillis();
                long next = -1;

                for (SelectRound r : allRoundsForTimer) {
                    try {
                        long start = sdf.parse(r.getStartTime()).getTime();
                        long end = sdf.parse(r.getEndTime()).getTime();
                        if (start > now && (next == -1 || start < next)) next = start;
                        if (end > now && (next == -1 || end < next)) next = end;
                    } catch (Exception ignore) { }
                }

                if (next > 0) {
                    delayMillis = Math.max(0, next - now) + 500;
                    System.out.println("[CoursePanel] 下次自动刷新在 " + delayMillis + " 毫秒后");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (delayMillis > Integer.MAX_VALUE) delayMillis = 60 * 60 * 1000;

        final long d = delayMillis;
        roundTimer = new javax.swing.Timer((int) Math.min(d, Integer.MAX_VALUE), e -> {
            if (roundTimer != null) {
                roundTimer.stop();
                roundTimer = null;
            }
            // ★ 先停掉轮询，避免两个定时器同时拉数据导致界面闪
            if (pollTimer != null) {
                pollTimer.stop();
                pollTimer = null;
            }
            System.out.println("[CoursePanel] 到点，自动刷新");
            loadRounds();
            loadCoursesFromServer();
            // ★ 重排轮询（如果面板还在显示）
            startPolling();
        });
        roundTimer.setRepeats(false);
        roundTimer.start();
    }

    @Override
    public void removeNotify() {
        if (roundTimer != null) {
            roundTimer.stop();
            roundTimer = null;
        }
        if (pollTimer != null) {
            pollTimer.stop();
            pollTimer = null;
        }
        super.removeNotify();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (allCourses.isEmpty()) loadCoursesFromServer();
    }

    private int calcCourseCardHeight(int classCount) {
        final int HEADER_HEIGHT = 44;
        final int ROW_HEIGHT = 40;
        final int ROW_GAP = 2;
        final int PADDING_TOP_BOTTOM = 12;

        int h = HEADER_HEIGHT + PADDING_TOP_BOTTOM + classCount * (ROW_HEIGHT + ROW_GAP);
        return Math.max(h, 140);
    }

    /**
     * ★ 轮询：用 SwingWorker 把网络请求挪到后台，不卡 EDT
     *    - 保留用户勾选状态
     *    - 检测轮次状态变化 → 全量重载
     *    - busy 时不打断提交/智能选课
     */
    private void startPolling() {
        if (pollTimer != null) pollTimer.stop();
        pollTimer = new javax.swing.Timer(POLL_INTERVAL_MS, e -> {
            if (busy) return;

            new SwingWorker<Void, Void>() {
                Map<String, Boolean> backupSelected = new HashMap<>(selectedMap);
                boolean roundsChanged = false;
                List<SelectRound> latestOpenRounds = new ArrayList<>();

                @Override
                protected Void doInBackground() {
                    // 1. 查最新所有轮次
                    try {
                        Message req = new Message();
                        req.setType(MsgConst.ADMIN_GET_ALL_ROUNDS);
                        req.setData(null);
                        Message resp = SocketClient.send(req);
                        if (resp != null && resp.isSuccess() && resp.getData() != null) {
                            @SuppressWarnings("unchecked")
                            List<SelectRound> all = (List<SelectRound>) resp.getData();

                            // 过滤出 open/upcoming，并排序
                            for (SelectRound r : all) {
                                if ("open".equals(r.getStatus()) || "upcoming".equals(r.getStatus())) {
                                    latestOpenRounds.add(r);
                                }
                            }
                            latestOpenRounds.sort((a, b) -> {
                                boolean ao = "open".equals(a.getStatus());
                                boolean bo = "open".equals(b.getStatus());
                                if (ao != bo) return ao ? -1 : 1;
                                String sa = a.getStartTime() != null ? a.getStartTime() : "";
                                String sb = b.getStartTime() != null ? b.getStartTime() : "";
                                return sa.compareTo(sb);
                            });

                            // ★ 比对本地 openRounds 和最新 latestOpenRounds
                            roundsChanged = !sameRoundList(openRounds, latestOpenRounds);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    // 2. 轮次列表没变，才拉课程数据
                    if (!roundsChanged) {
                        fetchCoursesFromServer();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    if (roundsChanged) {
                        // ★ 轮次列表变了：整体重载
                        System.out.println("[CoursePanel] 检测到轮次列表变化，重载轮次+课程");
                        loadRounds();            // loadRounds 里会自动 loadCoursesFromServer
                        return;
                    }

                    // 轮次没变：恢复勾选 + 重绘
                    selectedMap.clear();
                    for (CourseDisplayDTO dto : allCourses) {
                        String oid = dto.getOfferingId();
                        if (Boolean.TRUE.equals(backupSelected.get(oid))) {
                            selectedMap.put(oid, true);
                        }
                    }
                    updateTotalCount();
                    refreshCurrentView();
                    updateBottomStats();
                }
            }.execute();
        });
        pollTimer.setRepeats(true);
        pollTimer.start();
    }

    /**
     * 判断两个轮次列表是否等价（id + status 一一对应）
     */
    private boolean sameRoundList(List<SelectRound> a, List<SelectRound> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            SelectRound ra = a.get(i);
            SelectRound rb = b.get(i);
            if (!ra.getRoundId().equals(rb.getRoundId())) return false;
            if (!ra.getStatus().equals(rb.getStatus())) return false;
        }
        return true;
    }
}