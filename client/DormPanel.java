package vCampus.client;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import vCampus.common.Message;
import vCampus.common.enums.ApplicationStatus;
import vCampus.common.enums.FeeStatus;
import vCampus.common.enums.PaymentMethod;
import vCampus.common.enums.RepairStatus;
import vCampus.common.enums.StayStatus;
import vCampus.common.vo.Accommodation;
import vCampus.common.vo.DormApplication;
import vCampus.common.vo.DormFeeBill;
import vCampus.common.vo.DormRepair;
import vCampus.common.vo.Dormitory;
import vCampus.common.vo.Student;

public class DormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private Object currentUser;
    private String studentId;

    private CardLayout contentLayout;
    private JPanel contentPanel;

    private JButton selectedMenuButton;

    private JLabel lblDormId;
    private JLabel lblBuilding;
    private JLabel lblRoom;
    private JLabel lblBed;
    private JLabel lblCheckIn;
    private JLabel lblStatus;

    private DefaultTableModel dormModel;
    private DefaultTableModel applicationModel;
    private DefaultTableModel repairModel;
    private DefaultTableModel billModel;

    private JTable dormTable;
    private JTable applicationTable;
    private JTable repairTable;
    private JTable billTable;
    private JLabel billNoticeLabel;

    private static final Color PAGE_BG =
            new Color(247, 249, 250);

    private static final Color HEADER_GREEN =
            new Color(20, 120, 100);

    private static final Color BTN_GREEN =
            new Color(90, 170, 120);

    private static final Color LIGHT_GREEN =
            new Color(232, 244, 238);

    private static final Color CARD_BG =
            Color.WHITE;

    private static final Color BORDER_COLOR =
            new Color(210, 225, 220);

    private static final Color TEXT_GRAY =
            new Color(90, 100, 95);

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd");


    public DormPanel() {

        setLayout(new BorderLayout());
        setBackground(PAGE_BG);

        JLabel title =
                new JLabel(
                        "宿舍管理",
                        SwingConstants.CENTER
                );

        title.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.BOLD,
                        22
                )
        );

        add(title, BorderLayout.CENTER);
    }


    public DormPanel(Object user) {

        this.currentUser = user;

        setLayout(new BorderLayout());
        setBackground(PAGE_BG);

        if (user instanceof Student) {

            Student student =
                    (Student) user;

            this.studentId =
                    student.getSId();

            buildStudentUI();

        } else {

            buildUnsupportedUI();
        }
    }


    // =========================================================
    // 学生端总界面
    // =========================================================

    private void buildStudentUI() {

        add(
                createHeader(),
                BorderLayout.NORTH
        );

        JPanel body =
                new JPanel(
                        new BorderLayout()
                );

        body.setBackground(PAGE_BG);

        body.add(
                createSideMenu(),
                BorderLayout.WEST
        );

        contentLayout =
                new CardLayout();

        contentPanel =
                new JPanel(
                        contentLayout
                );

        contentPanel.setBackground(
                PAGE_BG
        );

        contentPanel.add(
                createMyDormPanel(),
                "MY_DORM"
        );

        contentPanel.add(
                createAvailableDormPanel(),
                "AVAILABLE"
        );

        contentPanel.add(
                createApplicationPanel(),
                "APPLICATION"
        );

        contentPanel.add(
                createRepairPanel(),
                "REPAIR"
        );

        contentPanel.add(
                createBillPanel(),
                "BILL"
        );

        body.add(
                contentPanel,
                BorderLayout.CENTER
        );

        add(
                body,
                BorderLayout.CENTER
        );

        loadMyDorm();
    }


    // =========================================================
    // 顶部标题
    // =========================================================

    private JPanel createHeader() {

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setBackground(
                HEADER_GREEN
        );

        header.setBorder(
                new EmptyBorder(
                        13,
                        24,
                        13,
                        24
                )
        );

        JLabel title =
                new JLabel(
                        "宿舍管理"
                );

        title.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.BOLD,
                        20
                )
        );

        title.setForeground(
                Color.WHITE
        );


        JLabel studentLabel =
                new JLabel(
                        "学生编号："
                        + studentId
                );

        studentLabel.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.PLAIN,
                        13
                )
        );

        studentLabel.setForeground(
                new Color(
                        230,
                        245,
                        240
                )
        );


        header.add(
                title,
                BorderLayout.WEST
        );

        header.add(
                studentLabel,
                BorderLayout.EAST
        );

        return header;
    }


    // =========================================================
    // 左侧二级导航
    // =========================================================

    private JPanel createSideMenu() {

        JPanel side =
                new JPanel();

        side.setLayout(
                new BoxLayout(
                        side,
                        BoxLayout.Y_AXIS
                )
        );

        side.setBackground(
                Color.WHITE
        );

        side.setPreferredSize(
                new Dimension(
                        145,
                        0
                )
        );

        side.setBorder(
                new LineBorder(
                        BORDER_COLOR,
                        1
                )
        );

        side.add(
                Box.createVerticalStrut(
                        24
                )
        );


        addMenuButton(
                side,
                "我的住宿",
                "MY_DORM"
        );

        addMenuButton(
                side,
                "可用宿舍",
                "AVAILABLE"
        );

        addMenuButton(
                side,
                "我的申请",
                "APPLICATION"
        );

        addMenuButton(
                side,
                "宿舍报修",
                "REPAIR"
        );

        addMenuButton(
                side,
                "住宿缴费",
                "BILL"
        );

        return side;
    }


    private void addMenuButton(
            JPanel parent,
            String text,
            String cardName) {

        JButton button =
                new JButton(text);

        button.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.PLAIN,
                        14
                )
        );

        button.setMaximumSize(
                new Dimension(
                        145,
                        42
                )
        );

        button.setPreferredSize(
                new Dimension(
                        145,
                        42
                )
        );

        button.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        button.setFocusPainted(false);

        button.setBorderPainted(false);

        button.setBackground(
                Color.WHITE
        );

        button.setForeground(
                TEXT_GRAY
        );

        button.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );


        button.addActionListener(
                e -> {

                    selectMenuButton(
                            button
                    );

                    contentLayout.show(
                            contentPanel,
                            cardName
                    );

                    refreshPage(
                            cardName
                    );
                }
        );


        parent.add(button);

        parent.add(
                Box.createVerticalStrut(
                        6
                )
        );


        if (selectedMenuButton
                == null) {

            selectMenuButton(
                    button
            );
        }
    }


    private void selectMenuButton(
            JButton button) {

        if (selectedMenuButton
                != null) {

            selectedMenuButton
                    .setBackground(
                            Color.WHITE
                    );

            selectedMenuButton
                    .setForeground(
                            TEXT_GRAY
                    );
        }

        selectedMenuButton =
                button;

        button.setBackground(
                LIGHT_GREEN
        );

        button.setForeground(
                HEADER_GREEN
        );
    }


    // =========================================================
    // 1 我的住宿
    // =========================================================

    private JPanel createMyDormPanel() {

        JPanel outer =
                createPagePanel();

        JPanel card =
                createCard();

        card.setLayout(
                new GridBagLayout()
        );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(
                        12,
                        15,
                        12,
                        15
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.anchor =
                GridBagConstraints.WEST;

        gbc.weightx =
                1;


        JLabel title =
                createCardTitle(
                        "我的住宿信息"
                );

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;

        card.add(
                title,
                gbc
        );

        gbc.gridwidth = 1;


        lblDormId =
                new JLabel("-");

        lblBuilding =
                new JLabel("-");

        lblRoom =
                new JLabel("-");

        lblBed =
                new JLabel("-");

        lblCheckIn =
                new JLabel("-");

        lblStatus =
                new JLabel("-");


        int row = 1;

        addInfoRow(
                card,
                gbc,
                row++,
                "宿舍编号：",
                lblDormId,
                "楼栋：",
                lblBuilding
        );

        addInfoRow(
                card,
                gbc,
                row++,
                "房间号：",
                lblRoom,
                "床位号：",
                lblBed
        );

        addInfoRow(
                card,
                gbc,
                row++,
                "入住时间：",
                lblCheckIn,
                "住宿状态：",
                lblStatus
        );



        outer.add(
                card,
                BorderLayout.NORTH
        );

        return outer;
    }


    private void loadMyDorm() {

        Accommodation acc =
                DormClientSrv
                        .queryMyDorm(
                                studentId
                        );

        if (acc == null) {

            lblDormId.setText("");
            lblBuilding.setText("");
            lblRoom.setText("");
            lblBed.setText("");
            lblCheckIn.setText("");

            lblStatus.setText(
                    "暂无住宿记录"
            );

            return;
        }

        String status =
                String.valueOf(
                        acc.getStatus()
                );

        if ("OUT".equals(status)) {

            lblDormId.setText("-");
            lblBuilding.setText("-");
            lblRoom.setText("-");
            lblBed.setText("-");
            lblCheckIn.setText("-");

            lblStatus.setText(
                    "OUT"
            );

            return;
        }

        lblDormId.setText(
                value(
                        acc.getDormId()
                )
        );

        lblBed.setText(
                value(
                        acc.getBedNumber()
                )
        );

        lblStatus.setText(
                status
        );

        if (acc.getCheckInTime()
                != null) {

            lblCheckIn.setText(
                    dateFormat.format(
                            acc.getCheckInTime()
                    )
            );

        } else {

            lblCheckIn.setText("");
        }

        List<Dormitory> dorms =
                DormClientSrv
                        .queryAllDorm();

        String building = "";
        String room = "";

        for (Dormitory dorm
                : dorms) {

            if (dorm.getDormId()
                    .equals(
                            acc.getDormId()
                    )) {

                building =
                        dorm.getBuilding();

                room =
                        dorm.getRoomNumber();

                break;
            }
        }

        lblBuilding.setText(
                value(building)
        );

        lblRoom.setText(
                value(room)
        );
    }


    // =========================================================
    // 2 可用宿舍
    // =========================================================

    private JPanel createAvailableDormPanel() {

        JPanel outer =
                createPagePanel();


        String[] columns = {

                "宿舍编号",
                "楼栋",
                "房间号",
                "总床位",
                "已入住",
                "剩余床位",
                "每学期费用"
        };


        dormModel =
                createReadOnlyModel(
                        columns
                );


        dormTable =
                createTable(
                        dormModel
                );


        JScrollPane scroll =
                createTableScroll(
                        dormTable
                );


        JPanel top =
                createSectionHeader(
                        "可用宿舍",
                        null,
                        null
                );


        outer.add(
                top,
                BorderLayout.NORTH
        );

        outer.add(
                scroll,
                BorderLayout.CENTER
        );

        return outer;
    }


    private void loadAvailableDorms() {

        List<Dormitory> dorms =
                DormClientSrv
                        .queryAvailableDorm();


        dormModel.setRowCount(0);


        for (Dormitory dorm
                : dorms) {

            dormModel.addRow(
                    new Object[] {

                            dorm.getDormId(),

                            dorm.getBuilding(),

                            dorm.getRoomNumber(),

                            dorm.getCapacity(),

                            dorm.getOccupied(),

                            dorm.getRemainingBeds(),

                            dorm.getFeePerSemester()
                    }
            );
        }
    }


    // =========================================================
    // 3 我的申请
    // =========================================================

    private JPanel createApplicationPanel() {

        JPanel outer =
                createPagePanel();


        String[] columns = {

                "申请编号",
                "类型",
                "当前宿舍",
                "目标宿舍",
                "目标床位",
                "原因",
                "状态",
                "提交时间"
        };


        applicationModel =
                createReadOnlyModel(
                        columns
                );


        applicationTable =
                createTable(
                        applicationModel
                );


        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        buttons.setBackground(
                PAGE_BG
        );

        JButton btnChange =
                createButton(
                        "申请调宿"
                );

        JButton btnCheckout =
                createButton(
                        "申请退宿"
                );

        JButton btnCancel =
                createButton(
                        "取消申请"
                );


        btnChange.addActionListener(
                e -> showChangeDormDialog()
        );

        btnCheckout.addActionListener(
                e -> showCheckoutDialog()
        );

        btnCancel.addActionListener(
                e -> cancelSelectedApplication()
        );


        buttons.add(
                btnChange
        );

        buttons.add(
                btnCheckout
        );

        buttons.add(
                btnCancel
        );

        outer.add(
                buttons,
                BorderLayout.NORTH
        );

        outer.add(
                createTableScroll(
                        applicationTable
                ),
                BorderLayout.CENTER
        );

        return outer;
    }


    private void loadApplications() {

        List<DormApplication> list =
                DormClientSrv
                        .queryMyApplications(
                                studentId
                        );


        applicationModel
                .setRowCount(0);


        for (DormApplication app
                : list) {

            applicationModel.addRow(
                    new Object[] {

                            app.getApplicationId(),

                            app.getType(),

                            app.getCurrentDormId(),

                            app.getTargetDormId(),

                            app.getTargetBedNumber(),

                            app.getReason(),

                            app.getStatus(),

                            formatDate(
                                    app.getSubmitTime()
                            )
                    }
            );
        }
    }


    // =========================================================
    // 4 宿舍报修
    // =========================================================

    private JPanel createRepairPanel() {

        JPanel outer =
                createPagePanel();


        String[] columns = {

                "报修编号",
                "宿舍",
                "问题描述",
                "状态",
                "提交时间",
                "更新时间"
        };


        repairModel =
                createReadOnlyModel(
                        columns
                );


        repairTable =
                createTable(
                        repairModel
                );


        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        buttons.setBackground(
                PAGE_BG
        );


        JButton btnSubmit =
                createButton(
                        "提交报修"
                );

        JButton btnCancel =
                createButton(
                        "取消报修"
                );


        btnSubmit.addActionListener(
                e -> showRepairDialog()
        );

        btnCancel.addActionListener(
                e -> cancelSelectedRepair()
        );


        buttons.add(
                btnSubmit
        );

        buttons.add(
                btnCancel
        );



        outer.add(
                buttons,
                BorderLayout.NORTH
        );

        outer.add(
                createTableScroll(
                        repairTable
                ),
                BorderLayout.CENTER
        );

        return outer;
    }


    private void loadRepairs() {

        List<DormRepair> list =
                DormClientSrv
                        .queryMyRepairs(
                                studentId
                        );


        repairModel
                .setRowCount(0);


        for (DormRepair repair
                : list) {

            repairModel.addRow(
                    new Object[] {

                            repair.getRepairId(),

                            repair.getDormId(),

                            repair.getDescription(),

                            repair.getStatus(),

                            formatDate(
                                    repair.getCreateTime()
                            ),

                            formatDate(
                                    repair.getUpdateTime()
                            )
                    }
            );
        }
    }


    // =========================================================
    // 5 住宿缴费
    // =========================================================

    private JPanel createBillPanel() {

        JPanel outer =
                createPagePanel();


        String[] columns = {

                "账单编号",
                "学年",
                "学期",
                "应缴金额",
                "已缴金额",
                "待缴金额",
                "截止日期",
                "状态"
        };


        billModel =
                createReadOnlyModel(
                        columns
                );


        billTable =
                createTable(
                        billModel
                );


        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );

        buttons.setBackground(
                PAGE_BG
        );


        JButton btnAll =
                createButton(
                        "全部账单"
                );

        JButton btnUnpaid =
                createButton(
                        "未缴账单"
                );

        JButton btnPay =
                createButton(
                        "缴费"
                );


        btnAll.addActionListener(
                e -> loadBills(false)
        );

        btnUnpaid.addActionListener(
                e -> loadBills(true)
        );

        btnPay.addActionListener(
                e -> paySelectedBill()
        );


        buttons.add(
                btnAll
        );

        buttons.add(
                btnUnpaid
        );

        buttons.add(
                btnPay
        );


        JPanel billTop =
                new JPanel(
                        new BorderLayout(
                                10,
                                8
                        )
                );

        billTop.setBackground(PAGE_BG);

        billTop.add(
                buttons,
                BorderLayout.NORTH
        );

        billNoticeLabel =
                new JLabel(
                        "支付方式：本人绑定银行卡"
                );

        billNoticeLabel.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.PLAIN,
                        13
                )
        );

        billNoticeLabel.setForeground(
                HEADER_GREEN
        );

        billTop.add(
                billNoticeLabel,
                BorderLayout.SOUTH
        );

        outer.add(
                billTop,
                BorderLayout.NORTH
        );

        outer.add(
                createTableScroll(
                        billTable
                ),
                BorderLayout.CENTER
        );

        return outer;
    }


    private void loadBills(
            boolean unpaidOnly) {

        List<DormFeeBill> list;


        if (unpaidOnly) {

            list =
                    DormClientSrv
                            .queryMyUnpaidBills(
                                    studentId
                            );

        } else {

            list =
                    DormClientSrv
                            .queryMyBills(
                                    studentId
                            );
        }


        billModel.setRowCount(0);

        int overdueCount = 0;
        java.util.Date now = new java.util.Date();

        for (DormFeeBill bill
                : list) {

            boolean overdue =
                    bill.getPaidAmount() < bill.getAmount()
                    && bill.getDueDate() != null
                    && bill.getDueDate().before(now);

            if (overdue) {
                overdueCount++;
            }

            String statusText =
                    overdue
                            ? FeeStatus.OVERDUE.toString()
                            : String.valueOf(
                                    bill.getStatus()
                            );

            billModel.addRow(
                    new Object[] {

                            bill.getBillId(),

                            bill.getAcademicYear(),

                            bill.getSemester(),

                            bill.getAmount(),

                            bill.getPaidAmount(),

                            bill.getUnpaidAmount(),

                            formatDate(
                                    bill.getDueDate()
                            ),

                            statusText
                    }
            );
        }

        if (billNoticeLabel != null) {
            if (overdueCount > 0) {
                billNoticeLabel.setText(
                        "⚠ 您有 "
                        + overdueCount
                        + " 笔住宿费已逾期，请尽快缴费。"
                );
                billNoticeLabel.setForeground(
                        new Color(190, 70, 60)
                );
            } else {
                billNoticeLabel.setText(
                        "支付方式：本人绑定银行卡"
                );
                billNoticeLabel.setForeground(
                        HEADER_GREEN
                );
            }
        }
    }


    // =========================================================
    // 学生缴费
    // =========================================================

    private String askBankPIN() {

        // 6 个分段密码框
        JPasswordField[] pinFields =
                new JPasswordField[6];

        // 显示 / 隐藏按钮
        JButton showButton =
                new JButton("显示");

        // 使用和 BankPanel 完全相同的 PIN 输入面板
        JPanel pinPanel =
                createPINPanel(
                        pinFields,
                        showButton
                );

        JPanel content =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                5,
                                5
                        )
                );

        content.add(
                new JLabel("银行卡密码:")
        );

        content.add(pinPanel);

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        content,
                        "请输入银行卡密码",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        String pin =
                getPIN(pinFields);

        if (!pin.matches("\\d{6}")
                || "000000".equals(pin)) {

            JOptionPane.showMessageDialog(
                    this,
                    "请输入正确的6位银行卡密码。"
            );

            return null;
        }

        return pin;
    }

    private JPanel createPINPanel(
            JPasswordField[] fields,
            JButton showButton) {

        JPanel panel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                3,
                                0
                        )
                );

        for (int i = 0; i < 6; i++) {

            JPasswordField field =
                    new JPasswordField(1);

            field.setHorizontalAlignment(
                    JPasswordField.CENTER
            );

            field.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            18
                    )
            );

            final int index = i;

            field.addKeyListener(
                    new java.awt.event.KeyAdapter() {

                        @Override
                        public void keyTyped(
                                java.awt.event.KeyEvent e) {

                            char c =
                                    e.getKeyChar();

                            if (!Character.isDigit(c)) {
                                e.consume();
                                return;
                            }
                            
                            if (field.getPassword().length > 0) {
                                e.consume();
                                return;
                            }

                            if (index < 5) {
                                SwingUtilities.invokeLater(
                                        () -> fields[index + 1]
                                                .requestFocusInWindow()
                                );
                            }
                        }

                        @Override
                        public void keyPressed(
                                java.awt.event.KeyEvent e) {

                            if (e.getKeyCode()
                                    == java.awt.event.KeyEvent.VK_BACK_SPACE
                                    && field.getPassword().length == 0
                                    && index > 0) {

                                fields[index - 1]
                                        .requestFocusInWindow();
                            }
                        }
                    }
            );

            fields[i] = field;

            panel.add(field);
        }

        showButton.addActionListener(e -> {

            boolean show =
                    showButton.getText()
                            .equals("显示");

            for (JPasswordField field : fields) {

                field.setEchoChar(
                        show ? (char) 0 : '•'
                );
            }

            showButton.setText(
                    show ? "隐藏" : "显示"
            );
        });

        panel.add(showButton);

        return panel;
    }

    private String getPIN(
            JPasswordField[] fields) {

        StringBuilder pin =
                new StringBuilder();

        for (JPasswordField field : fields) {
            pin.append(
                    field.getPassword()
            );
        }

        return pin.toString();
    }


    private void paySelectedBill() {

        int row =
                billTable
                        .getSelectedRow();

        if (row < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "请先选择一条账单。"
            );

            return;
        }

        int billId =
                ((Number)
                        billModel
                                .getValueAt(
                                        row,
                                        0
                                ))
                        .intValue();

        double unpaidAmount =
                ((Number)
                        billModel
                                .getValueAt(
                                        row,
                                        5
                                ))
                        .doubleValue();

        Object status =
                billModel
                        .getValueAt(
                                row,
                                7
                        );

        String statusText =
                String.valueOf(status);

        if (unpaidAmount <= 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "该账单无需缴费。"
            );

            return;
        }

        if (FeeStatus.PAID.toString()
                .equals(statusText)
                || FeeStatus.CANCELLED.toString()
                        .equals(statusText)) {

            JOptionPane.showMessageDialog(
                    this,
                    "当前账单不能进行缴费。"
            );

            return;
        }

        JTextField amountField =
                new JTextField(
                        String.format(
                                "%.2f",
                                unpaidAmount
                        )
                );

        JLabel paymentMethodLabel =
                new JLabel(
                        "本人绑定银行卡"
                );


        paymentMethodLabel.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.PLAIN,
                        13
                )
        );

        Object[] form = {

            "待缴金额：",
            String.format(
                    "%.2f 元",
                    unpaidAmount
            ),

            "本次缴费金额：",
            amountField,

            "支付方式：",
            paymentMethodLabel
        };

        int result =
                JOptionPane
                        .showConfirmDialog(
                                this,
                                form,
                                "住宿费缴费",
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.PLAIN_MESSAGE
                        );

        if (result
                != JOptionPane.OK_OPTION) {

            return;
        }

        double payAmount;

        try {

            payAmount =
                    Double.parseDouble(
                            amountField
                                    .getText()
                                    .trim()
                    );

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "请输入正确的缴费金额。"
            );

            return;
        }

        if (payAmount <= 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "缴费金额必须大于0。"
            );

            return;
        }

        if (payAmount > unpaidAmount) {

            JOptionPane.showMessageDialog(
                    this,
                    "缴费金额不能超过待缴金额。"
            );

            return;
        }

        PaymentMethod method =
                PaymentMethod.CAMPUS_BANK;

        String bankPIN =
                askBankPIN();

        if (bankPIN == null) {
            return;
        }

        Message response =
                DormClientSrv
                        .payBill(
                                studentId,
                                billId,
                                payAmount,
                                method,
                                bankPIN
                        );

        while (response != null
                && !response.isSuccess()
                && "银行卡密码错误，请重新输入。"
                        .equals(response.getResponseMsg())) {

            JOptionPane.showMessageDialog(
                    this,
                    "银行卡密码错误，请重新输入。",
                    "密码错误",
                    JOptionPane.WARNING_MESSAGE
            );

            bankPIN = askBankPIN();

            if (bankPIN == null) {
                return;
            }

            response =
                    DormClientSrv
                            .payBill(
                                    studentId,
                                    billId,
                                    payAmount,
                                    method,
                                    bankPIN
                            );
        }

        showResult(
                response
        );

        if (response != null
                && response.isSuccess()) {

            loadBills(false);
        }
    }


    // =========================================================
    // 调宿申请弹窗
    // =========================================================
    private void showChangeDormDialog() {

        List<Dormitory> dorms =
                DormClientSrv
                        .queryAvailableDorm();

        if (dorms.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "当前没有可用宿舍。"
            );

            return;
        }


        JComboBox<String> dormBox =
                new JComboBox<>();

        for (Dormitory dorm
                : dorms) {

            dormBox.addItem(
                    dorm.getDormId()
                    + " - "
                    + dorm.getBuilding()
                    + " "
                    + dorm.getRoomNumber()
            );
        }


        JComboBox<String> bedBox =
                new JComboBox<>();


        JTextArea reasonArea =
                new JTextArea(
                        4,
                        20
                );


        // =====================================================
        // 根据当前选择的宿舍加载可用床位
        // =====================================================

        Runnable loadBeds =
                () -> {

                    bedBox.removeAllItems();

                    int index =
                            dormBox.getSelectedIndex();

                    if (index < 0) {
                        return;
                    }

                    Dormitory dorm =
                            dorms.get(index);

                    List<String> beds =
                            DormClientSrv
                                    .queryAvailableBeds(
                                            dorm.getDormId()
                                    );

                    for (String bed
                            : beds) {

                        bedBox.addItem(
                                bed
                        );
                    }

                    if (beds.isEmpty()) {

                        bedBox.addItem(
                                "无可用床位"
                        );
                    }
                };


        // 初始加载
        loadBeds.run();


        // 切换宿舍时自动重新查询床位
        dormBox.addActionListener(
                e -> loadBeds.run()
        );


        Object[] form = {

                "目标宿舍：",
                dormBox,

                "目标床位：",
                bedBox,

                "申请原因：",
                new JScrollPane(
                        reasonArea
                )
        };


        int result =
                JOptionPane
                        .showConfirmDialog(
                                this,
                                form,
                                "申请调宿",
                                JOptionPane.OK_CANCEL_OPTION
                        );


        if (result
                != JOptionPane.OK_OPTION) {

            return;
        }


        int index =
                dormBox.getSelectedIndex();


        if (index < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "请选择目标宿舍。"
            );

            return;
        }


        Dormitory dorm =
                dorms.get(index);


        Object selectedBed =
                bedBox.getSelectedItem();


        if (selectedBed == null
                || "无可用床位".equals(
                        String.valueOf(
                                selectedBed
                        )
                )) {

            JOptionPane.showMessageDialog(
                    this,
                    "该宿舍当前没有可用床位。"
            );

            return;
        }


        String bed =
                String.valueOf(
                        selectedBed
                ).trim();


        String reason =
                reasonArea
                        .getText()
                        .trim();


        if (reason.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "申请原因不能为空。"
            );

            return;
        }


        Message response =
                DormClientSrv
                        .applyDormChange(
                                studentId,
                                dorm.getDormId(),
                                bed,
                                reason
                        );


        showResult(
                response
        );


        if (response != null
                && response.isSuccess()) {

            loadApplications();

            loadAvailableDorms();

            loadMyDorm();
        }
    }

    // =========================================================
    // 退宿申请弹窗
    // =========================================================

    private void showCheckoutDialog() {

        String[] options = {

                "在校生退宿",
                "毕业生退宿"
        };


        int type =
                JOptionPane
                        .showOptionDialog(
                                this,
                                "请选择退宿类型：",
                                "申请退宿",
                                JOptionPane.DEFAULT_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[0]
                        );


        if (type < 0) {
            return;
        }


        String reason =
                JOptionPane
                        .showInputDialog(
                                this,
                                "请输入退宿原因："
                        );


        if (reason == null
                || reason.trim()
                        .isEmpty()) {

            return;
        }


        vCampus.common.enums.ApplicationType
                applicationType;


        if (type == 0) {

            applicationType =
                    vCampus.common.enums
                            .ApplicationType
                            .CHECKOUT_CURRENT;

        } else {

            applicationType =
                    vCampus.common.enums
                            .ApplicationType
                            .CHECKOUT_GRADUATE;
        }


        Message response =
                DormClientSrv
                        .applyCheckout(
                                studentId,
                                applicationType,
                                reason.trim()
                        );


        showResult(
                response
        );


        if (response != null
                && response.isSuccess()) {

            loadApplications();

            loadMyDorm();
        }
    }


    // =========================================================
    // 取消申请
    // =========================================================

    private void cancelSelectedApplication() {

        int row =
                applicationTable
                        .getSelectedRow();


        if (row < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "请先选择一条申请记录。"
            );

            return;
        }


        int applicationId =
                ((Number)
                    applicationModel
                        .getValueAt(
                                row,
                                0
                        ))
                        .intValue();


        Object status =
                applicationModel
                        .getValueAt(
                                row,
                                6
                        );


        if (!ApplicationStatus
                .PENDING
                .toString()
                .equals(
                        String.valueOf(
                                status
                        )
                )) {

            JOptionPane.showMessageDialog(
                    this,
                    "只有待审核申请可以取消。"
            );

            return;
        }


        Message response =
                DormClientSrv
                        .cancelApplication(
                                studentId,
                                applicationId
                        );


        showResult(
                response
        );


        loadApplications();
    }


    // =========================================================
    // 提交报修
    // =========================================================

    private void showRepairDialog() {

        Accommodation acc =
                DormClientSrv
                        .queryMyDorm(
                                studentId
                        );

        if (acc == null
                || acc.getStatus()
                        != StayStatus.IN) {

            JOptionPane.showMessageDialog(
                    this,
                    "当前没有有效住宿记录，无法报修。"
            );

            return;
        }


        JTextArea description =
                new JTextArea(
                        5,
                        24
                );


        int result =
                JOptionPane
                        .showConfirmDialog(
                                this,
                                new JScrollPane(
                                        description
                                ),
                                "报修宿舍："
                                + acc.getDormId(),
                                JOptionPane.OK_CANCEL_OPTION
                        );


        if (result
                != JOptionPane.OK_OPTION) {

            return;
        }


        String text =
                description
                        .getText()
                        .trim();


        if (text.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "请输入故障描述。"
            );

            return;
        }


        Message response =
                DormClientSrv
                        .submitRepair(
                                studentId,
                                acc.getDormId(),
                                text
                        );


        showResult(
                response
        );


        if (response.isSuccess()) {

            loadRepairs();
        }
    }


    // =========================================================
    // 取消报修
    // =========================================================

    private void cancelSelectedRepair() {

        int row =
                repairTable
                        .getSelectedRow();


        if (row < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "请先选择一条报修记录。"
            );

            return;
        }


        int repairId =
                ((Number)
                    repairModel
                        .getValueAt(
                                row,
                                0
                        ))
                        .intValue();


        String status =
                String.valueOf(
                        repairModel
                                .getValueAt(
                                        row,
                                        3
                                )
                );


        if (!RepairStatus
                .PENDING
                .toString()
                .equals(status)) {

            JOptionPane.showMessageDialog(
                    this,
                    "只有待处理报修可以取消。"
            );

            return;
        }


        Message response =
                DormClientSrv
                        .cancelRepair(
                                studentId,
                                repairId
                        );


        showResult(
                response
        );


        loadRepairs();
    }


    // =========================================================
    // 页面刷新
    // =========================================================

    private void refreshPage(
            String cardName) {

        switch (cardName) {

        case "MY_DORM":

            loadMyDorm();
            break;

        case "AVAILABLE":

            loadAvailableDorms();
            break;

        case "APPLICATION":

            loadApplications();
            break;

        case "REPAIR":

            loadRepairs();
            break;

        case "BILL":

            loadBills(false);
            break;

        default:
            break;
        }
    }


    // =========================================================
    // 通用UI辅助
    // =========================================================

    private JPanel createPagePanel() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                10,
                                10
                        )
                );

        panel.setBackground(
                PAGE_BG
        );

        panel.setBorder(
                new EmptyBorder(
                        20,
                        22,
                        20,
                        22
                )
        );

        return panel;
    }


    private JPanel createCard() {

        JPanel card =
                new JPanel();

        card.setBackground(
                CARD_BG
        );

        card.setBorder(
                BorderFactory
                        .createCompoundBorder(

                            new LineBorder(
                                    BORDER_COLOR,
                                    1
                            ),

                            new EmptyBorder(
                                    18,
                                    18,
                                    18,
                                    18
                            )
                        )
        );

        return card;
    }


    private JLabel createCardTitle(
            String text) {

        JLabel label =
                new JLabel(
                        "■ " + text
                );

        label.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.BOLD,
                        16
                )
        );

        label.setForeground(
                HEADER_GREEN
        );

        return label;
    }


    private void addInfoRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label1,
            JLabel value1,
            String label2,
            JLabel value2) {

        gbc.gridx = 0;
        gbc.gridy = row;

        panel.add(
                new JLabel(label1),
                gbc
        );


        gbc.gridx = 1;

        panel.add(
                value1,
                gbc
        );


        gbc.gridx = 2;

        panel.add(
                new JLabel(label2),
                gbc
        );


        gbc.gridx = 3;

        panel.add(
                value2,
                gbc
        );
    }

    private JPanel createSectionHeader(
            String title,
            String buttonText,
            java.awt.event.ActionListener listener) {

        JPanel top =
                new JPanel(
                        new BorderLayout()
                );

        top.setBackground(
                PAGE_BG
        );

        JLabel label =
                createCardTitle(
                        title
                );

        top.add(
                label,
                BorderLayout.WEST
        );

        if (buttonText != null
                && listener != null) {

            JButton button =
                    createButton(
                            buttonText
                    );

            button.addActionListener(
                    listener
            );

            top.add(
                    button,
                    BorderLayout.EAST
            );
        }

        return top;
    }


    private JButton createButton(
            String text) {

        JButton button =
                new JButton(text);

        button.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.PLAIN,
                        13
                )
        );

        button.setBackground(
                BTN_GREEN
        );

        button.setForeground(
                Color.WHITE
        );

        button.setFocusPainted(false);

        button.setBorder(
                new EmptyBorder(
                        7,
                        16,
                        7,
                        16
                )
        );

        button.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );

        return button;
    }


    private DefaultTableModel
            createReadOnlyModel(
                    String[] columns) {

        return new DefaultTableModel(
                columns,
                0
        ) {

            private static final long
                    serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(
                    int row,
                    int column) {

                return false;
            }
        };
    }


    private JTable createTable(
            DefaultTableModel model) {

        JTable table =
                new JTable(model);

        table.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.PLAIN,
                        12
                )
        );

        table.setRowHeight(30);

        table.setSelectionMode(
                ListSelectionModel
                        .SINGLE_SELECTION
        );

        table.getTableHeader()
                .setFont(
                        new Font(
                                "Microsoft YaHei",
                                Font.BOLD,
                                12
                        )
                );

        table.getTableHeader()
                .setBackground(
                        LIGHT_GREEN
                );

        table.setGridColor(
                BORDER_COLOR
        );

        return table;
    }


    private JScrollPane createTableScroll(
            JTable table) {

        JScrollPane scroll =
                new JScrollPane(
                        table
                );

        scroll.setBorder(
                new LineBorder(
                        BORDER_COLOR,
                        1
                )
        );

        scroll.getViewport()
                .setBackground(
                        Color.WHITE
                );

        return scroll;
    }


    private void showResult(
            Message response) {

        if (response == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "服务器无响应。"
            );

            return;
        }


        JOptionPane.showMessageDialog(
                this,
                response.getResponseMsg(),
                response.isSuccess()
                    ? "操作成功"
                    : "操作失败",
                response.isSuccess()
                    ? JOptionPane
                        .INFORMATION_MESSAGE
                    : JOptionPane
                        .WARNING_MESSAGE
        );
    }


    private String formatDate(
            java.util.Date date) {

        if (date == null) {
            return "-";
        }

        return dateFormat.format(
                date
        );
    }


    private String value(
            String text) {

        return text == null
                ? "-"
                : text;
    }


    private void buildUnsupportedUI() {

        setLayout(
                new GridBagLayout()
        );

        setBackground(
                PAGE_BG
        );


        JLabel label =
                new JLabel(
                        "管理员宿舍管理界面将在下一阶段构建"
                );

        label.setFont(
                new Font(
                        "Microsoft YaHei",
                        Font.BOLD,
                        20
                )
        );

        label.setForeground(
                Color.GRAY
        );

        add(label);
    }
}