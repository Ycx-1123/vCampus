package vCampus.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import vCampus.common.Message;
import vCampus.common.vo.Admin;
import vCampus.common.vo.Book;
import vCampus.common.vo.BookCategory;
import vCampus.common.vo.BookCopy;
import vCampus.common.vo.BookRecommendation;
import vCampus.common.vo.BorrowRecord;
import vCampus.common.vo.ReservationRecord;
import vCampus.common.vo.Student;
import vCampus.common.vo.Teacher;

/** 图书馆模块界面。所有数据只通过 LibraryClientSrv 与服务端通信。 */
public class LibraryPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(245, 249, 252);
    private static final Color BLUE = new Color(47, 116, 177);
    private static final Color GREEN = new Color(58, 148, 104);
    private static final Color ORANGE = new Color(214, 135, 40);
    /** 显示给用户的预约时间格式，避免 LocalDateTime 默认字符串中的 T。 */
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Object currentUser;
    private final String readerId;
    private final boolean canBorrow;
    private final boolean canRecommend;
    private final boolean isAdmin;
    private final List<Book> books = new ArrayList<>();
    private final List<BorrowRecord> borrowRecords = new ArrayList<>();
    private final List<ReservationRecord> reservations = new ArrayList<>();
    private final List<BookRecommendation> recommendations = new ArrayList<>();

    private static final int MAX_BORROWED_BOOKS = 3;
    private static final double OVERDUE_FINE_PER_DAY = 0.10D;
    private static final double DAMAGE_FINE = 20.00D;

    private final DefaultTableModel bookModel = model(new String[] { "编号", "书名", "作者", "出版社", "分类", "出版年份", "馆藏总册数", "可借" });
    private final DefaultTableModel borrowModel = model(new String[] { "借阅编号", "书名", "副本", "借阅日期", "应还日期", "归还日期", "状态" });
    private final DefaultTableModel reservationModel = model(new String[] { "预约编号", "书名", "排队序号", "预约时间", "取书截止", "状态" });
    private final DefaultTableModel copyModel = model(new String[] { "副本编号", "位置", "入库日期", "状态", "流通方式", "备注" });
    private final DefaultTableModel recommendationModel = model(new String[] { "编号", "书名", "作者", "出版社", "图书分类号", "推荐理由", "提交时间", "状态" });
    private final DefaultTableModel procurementModel = model(new String[] { "书名", "作者", "出版社", "图书分类号", "教师荐书", "学生荐书", "有效预约", "需求等级", "建议增购", "采购参考分" });

    private JTable bookTable;
    private JTable borrowTable;
    private JTable reservationTable;
    private JTextField keywordField;
    private JComboBox<String> categoryFilter;
    private JPanel searchTagPanel;
    private final List<SearchTag> searchTags = new ArrayList<>();
    private final Map<String, String> categoryIds = new LinkedHashMap<>();
    private JLabel statusLabel;
    private JTable copyTable;
    private JLabel selectedBookLabel;
    private JLabel borrowDateLabel;
    private JLabel dueDateLabel;
    private JTable recommendationTable;
    private boolean procurementLoading;
    private javax.swing.Timer procurementTimer;
    /** 当前可见页签每 3 秒静默同步一次，避免用户手动刷新。 */
    private javax.swing.Timer liveRefreshTimer;
    private JTabbedPane tabs;
    private boolean booksLoading;
    private boolean borrowRecordsLoading;
    private boolean reservationsLoading;
    private boolean recommendationsLoading;
    private JTextField recommendationBookField;
    private JTextField recommendationAuthorField;
    private JTextField recommendationPublisherField;
    /** 荐书使用中图法分类号。 */
    private JComboBox<String> recommendationCategoryBox;
    private JTextArea recommendationReasonField;
    private final List<BookCopy> borrowableCopies = new ArrayList<>();

    public LibraryPanel() {
        this(null);
    }

    public LibraryPanel(Object currentUser) {
        this.currentUser = currentUser;
        this.readerId = readerIdOf(currentUser);
        this.canBorrow = currentUser instanceof Student || currentUser instanceof Teacher;
        this.canRecommend = currentUser instanceof Student || currentUser instanceof Teacher;
        this.isAdmin = currentUser instanceof Admin;
        initUI();
        loadBookCategories();
        loadBooks();
        if (canBorrow) {
            // 登录后直接显示个人记录，不需要再手动点击“刷新”。
            loadBorrowRecords();
            loadReservations();
        }
        if (canRecommend) loadMyRecommendations();
        if (isAdmin) loadProcurementReferences();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        add(header(), BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setFont(font(Font.BOLD, 14));
        tabs.addTab("图书检索", bookPanel());
        if (canBorrow) {
            JPanel borrow = borrowPanel();
            JPanel reservation = reservationPanel();
            tabs.addTab("我的借阅", borrow);
            tabs.addTab("我的预约", reservation);
            // 进入个人页面即自动同步，无需额外“刷新”按钮。
            tabs.addChangeListener(e -> {
                if (tabs.getSelectedComponent() == borrow) loadBorrowRecords();
                else if (tabs.getSelectedComponent() == reservation) loadReservations();
            });
        }
        if (canRecommend) {
            JPanel recommendation = recommendationPanel();
            tabs.addTab("师生荐书", recommendation);
            // 每次进入该页签自动同步记录，读者不需要手动点击刷新。
            tabs.addChangeListener(e -> {
                if (tabs.getSelectedComponent() == recommendation) loadMyRecommendations();
            });
        }
        if (isAdmin) tabs.addTab("书目管理", managePanel());
        if (isAdmin && ((Admin) currentUser).isManageLibrary()) {
            tabs.addTab("借阅管理", new LibraryAdminPanel((Admin) currentUser, this::loadBooks));
        }
        if (isAdmin) tabs.addTab("荐书采购参考", procurementPanel());
        tabs.addTab("借阅规则", rulesPanel());
        tabs.addChangeListener(e -> refreshActiveTab());
        add(tabs, BorderLayout.CENTER);

        statusLabel = new JLabel("正在连接图书馆服务…");
        statusLabel.setFont(font(Font.PLAIN, 12));
        statusLabel.setBorder(new EmptyBorder(6, 16, 8, 16));
        add(statusLabel, BorderLayout.SOUTH);
        installLiveRefresh();
    }

    /**
     * 没有使用服务端推送时，以短间隔轮询实现多人界面的“近实时”同步。
     * 只刷新当前打开的页签，避免在网络较慢时制造无意义请求。
     */
    private void installLiveRefresh() {
        liveRefreshTimer = new javax.swing.Timer(3000, e -> refreshActiveTab());
        liveRefreshTimer.setCoalesce(true);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (isShowing()) {
                refreshActiveTab();
                liveRefreshTimer.start();
            } else {
                liveRefreshTimer.stop();
            }
        });
    }

    /** 刷新当前正在查看的数据；采购参考和管理员借阅页有各自的轮询器。 */
    private void refreshActiveTab() {
        if (!isShowing() || tabs == null || tabs.getSelectedIndex() < 0) return;
        String title = tabs.getTitleAt(tabs.getSelectedIndex());
        if ("图书检索".equals(title) || "书目管理".equals(title)) loadBooks();
        else if ("我的借阅".equals(title)) loadBorrowRecords();
        else if ("我的预约".equals(title)) loadReservations();
        else if ("师生荐书".equals(title)) loadMyRecommendations();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLUE);
        panel.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel("图书馆");
        title.setForeground(Color.WHITE);
        title.setFont(font(Font.BOLD, 24));
        panel.add(title, BorderLayout.WEST);
        String text = isAdmin ? "图书馆管理员" : "读者编号：" + (readerId == null ? "未识别" : readerId);
        JLabel user = new JLabel(text);
        user.setForeground(new Color(235, 244, 252));
        user.setFont(font(Font.PLAIN, 13));
        panel.add(user, BorderLayout.EAST);
        return panel;
    }

    private JPanel bookPanel() {
        JPanel panel = basePanel();
        JPanel tools = new JPanel();
        tools.setLayout(new BoxLayout(tools, BoxLayout.X_AXIS));
        tools.setBackground(Color.WHITE);
        tools.setBorder(new EmptyBorder(8, 12, 8, 12));
        tools.add(label("关键词："));
        tools.add(Box.createHorizontalStrut(8));
        keywordField = new JTextField(13);
        keywordField.setMaximumSize(new Dimension(145, 30));
        keywordField.addActionListener(e -> addSearchTag());
        tools.add(keywordField);
        tools.add(Box.createHorizontalStrut(8));
        JButton search = button("检索", BLUE);
        search.addActionListener(e -> addSearchTag());
        tools.add(search);
        tools.add(Box.createHorizontalStrut(14));
        tools.add(label("分类："));
        tools.add(Box.createHorizontalStrut(8));
        categoryFilter = new JComboBox<>();
        categoryFilter.setPreferredSize(new Dimension(120, 30));
        categoryFilter.setMaximumSize(new Dimension(120, 30));
        categoryFilter.addItem("全部分类");
        categoryFilter.addActionListener(e -> loadBooks());
        tools.add(categoryFilter);
        if (canBorrow) {
            tools.add(Box.createHorizontalStrut(8));
            JButton reserve = button("预约", ORANGE);
            reserve.addActionListener(e -> reserveBook());
            tools.add(reserve);
        }

        bookTable = new JTable(bookModel);
        configureTable(bookTable);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(185);
        bookTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showCopies(selectedBook());
        });

        copyTable = new JTable(copyModel);
        configureTable(copyTable);
        searchTagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        searchTagPanel.setBackground(Color.WHITE);
        searchTagPanel.setBorder(new EmptyBorder(0, 12, 8, 12));
        searchTagPanel.setVisible(false);
        JPanel searchArea = new JPanel(new BorderLayout());
        searchArea.setBackground(Color.WHITE);
        searchArea.add(tools, BorderLayout.NORTH);
        searchArea.add(searchTagPanel, BorderLayout.SOUTH);
        panel.add(searchArea, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(bookTable), detailPanel());
        split.setResizeWeight(0.62);
        split.setBorder(null);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    /** 面向读者公开展示的规则，避免用户必须到其他页面或询问管理员才能得知限制。 */
    private JPanel rulesPanel() {
        JPanel panel = basePanel();
        JTextArea rules = new JTextArea(
                "图书馆借阅规则\n\n"
                + "1. 借阅资格：学生、教师须使用本人有效校园卡，并已开通图书馆读者权限。\n"
                + "2. 借阅数量：每位读者手头同时在借图书最多 " + MAX_BORROWED_BOOKS + " 册；本系统逐册选择实体副本并确认借阅。\n"
                + "3. 借阅期限：普通图书借期为 30 天；有效预约达到 3 人的热门书借期缩短为 14 天，应还日期由系统自动计算。\n"
                + "4. 续借：只能在应还日前 10 天内办理；每册最多续借 2 次，每次延长 30 天。\n"
                + "   图书已逾期、已达到续借上限或已有读者预约排队时，不能续借；热门书不可续借。\n"
                + "5. 预约：选中书目后，如当前没有空闲的可外借副本，即可点击“预约”加入排队；按提交时间排序。\n"
                + "   图书到馆并显示“已到馆”后，\n"
                + "   请在 7 天内到“我的预约”办理借阅；超期未办理将自动失效，并按队列通知下一位读者。\n"
                + "   每位读者最多同时保留 3 个有效预约（排队中或已到馆）；同一本书只能预约一次。\n"
                + "6. 热门采购：有效预约达到 3 人提示增购 1 册，达到 6 人提示增购 2 册，达到 10 人列为紧急采购并提示增购 3 册。\n"
                + "7. 馆藏说明：馆藏总册数指图书馆登记的实体副本总数；当前可借指状态为“可借”的副本数。\n"
                + "   馆藏总册数不等于当前可借数，已借出、已预约、损坏或遗失副本均可能不可外借。\n"
                + "8. 逾期处理：超过应还日期归还的，每册每天收取 " + String.format("%.2f", OVERDUE_FINE_PER_DAY)
                + " 元逾期占用费；归还时从校园一卡通余额自动扣除。余额不足时，请前往管理员处办理还书并处理欠费。\n"
                + "9. 损坏处理：归还时登记为损坏的副本将停止外借；系统从校园一卡通扣除 "
                + String.format("%.2f", DAMAGE_FINE) + " 元损坏处理费，并禁借 7 天。\n"
                + "10. 禁借期间不能借书、续借或预约，但可归还已借图书。\n"
                + "11. 请妥善保管图书；遗失、严重损坏等未处理事项由管理员按学校规定另行处理。\n");
        rules.setEditable(false);
        rules.setFont(font(Font.PLAIN, 14));
        rules.setLineWrap(true);
        rules.setWrapStyleWord(true);
        rules.setBackground(Color.WHITE);
        rules.setBorder(new EmptyBorder(14, 18, 14, 18));
        panel.add(new JScrollPane(rules), BorderLayout.CENTER);
        return panel;
    }

    /** 图书详情与借阅表单固定显示在页面中，避免连续弹出多个对话框。 */
    private JPanel detailPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        selectedBookLabel = new JLabel("请在上方选中书目，查看副本并办理借阅。");
        selectedBookLabel.setFont(font(Font.BOLD, 14));
        JPanel detailHeader = new JPanel(new BorderLayout(0, 4));
        detailHeader.setOpaque(false);
        detailHeader.add(selectedBookLabel, BorderLayout.NORTH);
        JTextArea process = new JTextArea("借阅流程：1. 在上方表格选择书目；2. 在副本表选择“可外借、可借”的副本；3. 点击“确认借阅”。若当前没有空闲的可外借副本，可直接点击上方“预约”按钮排队。馆内阅览副本不可外借。 ");
        process.setEditable(false);
        process.setLineWrap(true);
        process.setWrapStyleWord(true);
        process.setFont(font(Font.PLAIN, 12));
        process.setForeground(new Color(83, 101, 115));
        process.setOpaque(false);
        process.setBorder(new EmptyBorder(0, 0, 2, 0));
        detailHeader.add(process, BorderLayout.CENTER);
        panel.add(detailHeader, BorderLayout.NORTH);

        JScrollPane copies = new JScrollPane(copyTable);
        copies.setPreferredSize(new Dimension(700, 115));
        panel.add(copies, BorderLayout.CENTER);

        // 操作按钮单独占一行；窗口较窄时不会被右侧裁掉。
        JPanel form = new JPanel(new BorderLayout(8, 4));
        form.setBackground(new Color(238, 246, 252));
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(196, 217, 232)), new EmptyBorder(6, 8, 6, 8)));
        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        fields.setOpaque(false);
        fields.add(label("借阅日期："));
        borrowDateLabel = new JLabel("—");
        fields.add(borrowDateLabel);
        fields.add(label("应还日期："));
        dueDateLabel = new JLabel("—（系统按预约人数确定 14 或 30 天借期）");
        fields.add(dueDateLabel);
        form.add(fields, BorderLayout.CENTER);
        // 管理员不在书目检索页直接借书，统一通过“借阅管理”按用户 ID 代办。
        if (canBorrow) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            actions.setOpaque(false);
            JButton confirm = button("确认借阅", GREEN);
            confirm.addActionListener(e -> confirmBorrow());
            actions.add(confirm);
            form.add(actions, BorderLayout.SOUTH);
        }
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel borrowPanel() {
        JPanel panel = basePanel();
        JPanel tools = toolPanel();
        JButton returnBook = button("归还", GREEN);
        returnBook.addActionListener(e -> returnBook());
        tools.add(returnBook);
        JButton returnDamaged = button("归还并登记损坏", new Color(175, 85, 64));
        returnDamaged.addActionListener(e -> returnDamagedBook());
        tools.add(returnDamaged);
        JButton renew = button("续借（最多2次）", ORANGE);
        renew.addActionListener(e -> renewBook());
        tools.add(renew);
        borrowTable = new JTable(borrowModel);
        configureTable(borrowTable);
        panel.add(tools, BorderLayout.NORTH);
        panel.add(new JScrollPane(borrowTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel reservationPanel() {
        JPanel panel = basePanel();
        JPanel tools = toolPanel();
        JButton cancel = button("取消预约", ORANGE);
        cancel.addActionListener(e -> cancelReservation());
        tools.add(cancel);
        JButton borrowReady = button("借阅已到馆图书", GREEN);
        borrowReady.addActionListener(e -> borrowReadyReservation());
        tools.add(borrowReady);
        reservationTable = new JTable(reservationModel);
        configureTable(reservationTable);
        panel.add(tools, BorderLayout.NORTH);
        panel.add(new JScrollPane(reservationTable), BorderLayout.CENTER);
        return panel;
    }

    /** 教师填写完整荐书信息的固定界面，不使用逐项弹窗。 */
    private JPanel recommendationPanel() {
        JPanel panel = basePanel();
        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("填写推荐图书清单"), new EmptyBorder(8, 10, 8, 10)));

        JPanel fields = new JPanel(new GridLayout(2, 4, 8, 8));
        fields.setBackground(Color.WHITE);
        recommendationBookField = new JTextField();
        recommendationAuthorField = new JTextField();
        recommendationPublisherField = new JTextField();
        recommendationCategoryBox = new JComboBox<>();
        recommendationCategoryBox.setToolTipText("请选择图书分类号，例如 TP311（Java 程序设计）");
        fields.add(label("书名（必填）：")); fields.add(recommendationBookField);
        fields.add(label("作者：")); fields.add(recommendationAuthorField);
        fields.add(label("出版社：")); fields.add(recommendationPublisherField);
        fields.add(label("图书分类号：")); fields.add(recommendationCategoryBox);
        form.add(fields, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBackground(Color.WHITE);
        bottom.add(label("推荐理由（最多 100 字）："), BorderLayout.WEST);
        recommendationReasonField = new JTextArea(3, 20);
        recommendationReasonField.setLineWrap(true);
        recommendationReasonField.setWrapStyleWord(true);
        JScrollPane reasonScroll = new JScrollPane(recommendationReasonField);
        reasonScroll.setPreferredSize(new Dimension(480, 70));
        bottom.add(reasonScroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setBackground(Color.WHITE);
        JButton submit = button("提交荐书", GREEN);
        submit.addActionListener(e -> submitRecommendation());
        actions.add(submit);
        bottom.add(actions, BorderLayout.EAST);
        form.add(bottom, BorderLayout.SOUTH);

        recommendationTable = new JTable(recommendationModel);
        configureTable(recommendationTable);
        recommendationTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        recommendationTable.getColumnModel().getColumn(5).setPreferredWidth(180);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(recommendationTable), BorderLayout.CENTER);
        return panel;
    }

    /** 管理员查看下一次采购的参考排序，并根据有效预约数显示热门和紧急采购建议。 */
    private JPanel procurementPanel() {
        JPanel panel = basePanel();
        JTextArea tip = helpText("热门规则：有效预约达到 3 人建议增购 1 册，达到 6 人建议增购 2 册，达到 10 人列为紧急采购并建议增购 3 册。预约人数较多的现有馆藏会自动进入本表；页面每 5 秒自动更新。");
        JTable table = new JTable(procurementModel);
        configureTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        panel.add(tip, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        procurementTimer = new javax.swing.Timer(5000, e -> {
            if (panel.isShowing()) loadProcurementReferences();
        });
        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (panel.isShowing()) {
                    loadProcurementReferences();
                    procurementTimer.start();
                } else procurementTimer.stop();
            }
        });
        return panel;
    }

    private JPanel managePanel() {
        JPanel panel = basePanel();
        JPanel tools = toolPanel();
        JButton add = button("新增书目", GREEN);
        add.addActionListener(e -> editBook(null));
        tools.add(add);
        JButton edit = button("修改选中书目", BLUE);
        edit.addActionListener(e -> editBook(selectedBook()));
        tools.add(edit);
        JButton delete = button("删除选中书目", ORANGE);
        delete.addActionListener(e -> deleteBook());
        tools.add(delete);
        JButton addCopy = button("新增副本", new Color(75, 142, 125));
        addCopy.addActionListener(e -> addCopy());
        tools.add(addCopy);
        tools.setLayout(new GridLayout(2, 2, 8, 6));
        JPanel toolbar = new JPanel(new BorderLayout(0, 6));
        toolbar.setOpaque(false);
        toolbar.add(tools, BorderLayout.NORTH);
        toolbar.add(helpText("先在下方表格选择书目，再修改书目或新增副本。副本数量由系统自动汇总；分类从已有分类中选择。"), BorderLayout.CENTER);
        JTable table = new JTable(bookModel);
        configureTable(table);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0 && bookTable != null) {
                bookTable.setRowSelectionInterval(table.getSelectedRow(), table.getSelectedRow());
            }
        });
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadBooks() {
        if (booksLoading) return;
        booksLoading = true;
        Book current = selectedBook();
        final String selectedBookId = current == null ? null : current.getBookId();
        load("正在查询图书…", LibraryClientSrv::getAllBooks, result -> {
            String selectedCategory = categoryFilter == null ? "全部分类" : (String) categoryFilter.getSelectedItem();
            String categoryId = categoryIds.get(selectedCategory);
            books.clear(); bookModel.setRowCount(0);
            for (Book b : result) {
                if (categoryId != null && !categoryId.equals(b.getCategoryId())) continue;
                if (!matchesAllSearchTags(b)) continue;
                books.add(b);
                bookModel.addRow(new Object[] { b.getBookId(), b.getBookName(), b.getbAuthor(), b.getbPublisher(), categoryText(b.getCategoryId()), b.getPublishYear(), b.getbTotal(), b.getbAvailable() });
            }
            if (selectedBookId != null && bookTable != null) {
                for (int i = 0; i < books.size(); i++) {
                    if (selectedBookId.equals(books.get(i).getBookId())) {
                        bookTable.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
            status(books.isEmpty() ? "未查询到符合条件的图书。" : "已加载 " + books.size() + " 条书目。", false);
        }, () -> booksLoading = false);
    }

    /** 将输入的条件保存为可删除标签，而不是让新输入覆盖上一次检索条件。 */
    private void addSearchTag() {
        String keyword = keywordField == null ? "" : keywordField.getText().trim();
        if (keyword.isEmpty()) {
            loadBooks();
            return;
        }
        searchTags.add(new SearchTag("关键词", keyword));
        keywordField.setText("");
        refreshSearchTags();
        loadBooks();
    }

    private void refreshSearchTags() {
        if (searchTagPanel == null) return;
        searchTagPanel.removeAll();
        for (SearchTag tag : searchTags) {
            JButton chip = new JButton(tag.keyword + "  ×");
            chip.setFont(font(Font.PLAIN, 12));
            chip.setForeground(new Color(49, 98, 137));
            chip.setBackground(new Color(226, 240, 249));
            chip.setFocusPainted(false);
            chip.setBorder(new EmptyBorder(4, 8, 4, 8));
            chip.setCursor(new Cursor(Cursor.HAND_CURSOR));
            chip.setToolTipText("点击删除此检索条件");
            chip.addActionListener(e -> { searchTags.remove(tag); refreshSearchTags(); loadBooks(); });
            searchTagPanel.add(chip);
        }
        searchTagPanel.setVisible(!searchTags.isEmpty());
        searchTagPanel.revalidate();
        searchTagPanel.repaint();
    }

    private boolean matchesAllSearchTags(Book book) {
        for (SearchTag tag : searchTags) if (!matchesSearchTag(book, tag)) return false;
        return true;
    }

    private boolean matchesSearchTag(Book book, SearchTag tag) {
        return containsIgnoreCase(book.getBookName(), tag.keyword)
                || containsIgnoreCase(book.getbAuthor(), tag.keyword)
                || containsIgnoreCase(book.getbPublisher(), tag.keyword)
                || (book.getPublishYear() != null && String.valueOf(book.getPublishYear()).contains(tag.keyword));
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return safe(text).toLowerCase().contains(safe(keyword).toLowerCase());
    }

    private void loadBookCategories() {
        load("正在加载图书分类…", LibraryClientSrv::getAllBookCategories, result -> {
            Object selected = categoryFilter == null ? null : categoryFilter.getSelectedItem();
            categoryIds.clear();
            if (categoryFilter != null) {
                categoryFilter.removeAllItems();
                categoryFilter.addItem("全部分类");
            }
            for (BookCategory category : result) {
                String text = safe(category.getCategoryName()) + "（" + safe(category.getCategoryId()) + "）";
                categoryIds.put(text, category.getCategoryId());
                if (categoryFilter != null) categoryFilter.addItem(text);
            }
            if (selected != null && categoryFilter != null) categoryFilter.setSelectedItem(selected);
            refreshRecommendationCategoryOptions();
        });
    }

    /** 荐书选择现有图书分类号；显示分类名称以帮助师生做出正确选择。 */
    private void refreshRecommendationCategoryOptions() {
        if (recommendationCategoryBox == null) return;
        Object selected = recommendationCategoryBox.getSelectedItem();
        recommendationCategoryBox.removeAllItems();
        recommendationCategoryBox.addItem("请选择图书分类号");
        for (String categoryText : categoryIds.keySet()) recommendationCategoryBox.addItem(categoryText);
        if (selected != null) recommendationCategoryBox.setSelectedItem(selected);
    }

    private String recommendationCategoryId() {
        if (recommendationCategoryBox == null) return "";
        Object selected = recommendationCategoryBox.getSelectedItem();
        return selected == null ? "" : safe(categoryIds.get(selected.toString()));
    }

    private void loadBorrowRecords() {
        if (borrowRecordsLoading) return;
        borrowRecordsLoading = true;
        BorrowRecord current = selectedBorrowRecord();
        final String selectedRecordId = current == null ? null : current.getRecordId();
        load("正在查询借阅记录…", () -> LibraryClientSrv.getMyBorrowRecords(readerId), result -> {
            borrowRecords.clear(); borrowRecords.addAll(result); borrowModel.setRowCount(0);
            for (int i = 0; i < result.size(); i++) {
                BorrowRecord r = result.get(i);
                borrowModel.addRow(new Object[] { displayId(r.getRecordId(), "BR", i + 1), bookText(r.getBookName(), r.getBookId()),
                        r.getCopyId(), r.getBorrowDate(), r.getDueDate(), r.getReturnDate(), borrowStatus(r.getStatus()) });
            }
            if (selectedRecordId != null && borrowTable != null) {
                for (int i = 0; i < borrowRecords.size(); i++) {
                    if (selectedRecordId.equals(borrowRecords.get(i).getRecordId())) {
                        borrowTable.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
            status("已加载 " + result.size() + " 条借阅记录。", false);
        }, () -> borrowRecordsLoading = false);
    }

    private void loadReservations() {
        if (reservationsLoading) return;
        reservationsLoading = true;
        ReservationRecord current = selectedReservation();
        final String selectedReservationId = current == null ? null : current.getReservationId();
        load("正在查询预约记录…", () -> LibraryClientSrv.getMyReservations(readerId), result -> {
            reservations.clear(); reservations.addAll(result); reservationModel.setRowCount(0);
            for (int i = 0; i < result.size(); i++) {
                ReservationRecord r = result.get(i);
                reservationModel.addRow(new Object[] { displayId(r.getReservationId(), "RS", i + 1), bookText(r.getBookName(), r.getBookId()),
                        r.getQueueNumber(), reservationTimeText(r.getReservationTime()), readyDeadlineText(r), reservationStatus(r.getStatus()) });
            }
            if (selectedReservationId != null && reservationTable != null) {
                for (int i = 0; i < reservations.size(); i++) {
                    if (selectedReservationId.equals(reservations.get(i).getReservationId())) {
                        reservationTable.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
            status("已加载 " + result.size() + " 条预约记录。", false);
        }, () -> reservationsLoading = false);
    }

    private void loadMyRecommendations() {
        if (recommendationsLoading) return;
        recommendationsLoading = true;
        load("正在查询我的荐书记录…", () -> LibraryClientSrv.getMyBookRecommendations(readerId), result -> {
            recommendations.clear(); recommendations.addAll(result); recommendationModel.setRowCount(0);
            for (BookRecommendation item : result) {
                recommendationModel.addRow(new Object[] { displayId(item.getRecommendationId(), "RC", recommendationModel.getRowCount() + 1),
                        item.getBookName(), item.getAuthor(), item.getPublisher(), item.getCategoryId(), item.getReason(),
                        reservationTimeText(item.getRecommendTime()), recommendationStatus(item.getStatus()) });
            }
            status("已加载 " + result.size() + " 条我的荐书记录。", false);
        }, () -> recommendationsLoading = false);
    }

    private void loadProcurementReferences() {
        if (procurementLoading) return;
        procurementLoading = true;
        new SwingWorker<List<BookRecommendation>, Void>() {
            protected List<BookRecommendation> doInBackground() {
                return LibraryClientSrv.getProcurementReferences();
            }
            protected void done() {
                try {
                    List<BookRecommendation> result = get();
                    procurementModel.setRowCount(0);
                    for (BookRecommendation item : result) {
                        procurementModel.addRow(new Object[] { item.getBookName(), item.getAuthor(), item.getPublisher(), item.getCategoryId(),
                                item.getTeacherRecommendationCount(), item.getStudentRecommendationCount(), item.getActiveReservationCount(),
                                item.getDemandLevel(), item.getSuggestedAdditionalCopies() == 0 ? "暂不增购" : "增购 " + item.getSuggestedAdditionalCopies() + " 册",
                                item.getReferenceScore() });
                    }
                } catch (Exception e) {
                    status("采购参考自动更新失败：" + e.getMessage(), true);
                } finally { procurementLoading = false; }
            }
        }.execute();
    }

    private void submitRecommendation() {
        String bookName = recommendationBookField == null ? "" : recommendationBookField.getText().trim();
        if (bookName.isEmpty()) { hint("荐书信息填写不完整：请填写书名。"); return; }
        String reason = recommendationReasonField == null ? "" : recommendationReasonField.getText().trim();
        if (reason.length() > 100) { hint("推荐理由最多填写 100 个字。"); return; }
        BookRecommendation recommendation = new BookRecommendation();
        recommendation.setReaderCardId(readerId);
        recommendation.setBookName(bookName);
        recommendation.setAuthor(recommendationAuthorField.getText().trim());
        recommendation.setPublisher(recommendationPublisherField.getText().trim());
        recommendation.setCategoryId(recommendationCategoryId());
        recommendation.setReason(reason);
        recommendation.setRecommendTime(LocalDateTime.now());
        action("正在提交荐书…", () -> LibraryClientSrv.submitBookRecommendation(recommendation), () -> {
            recommendationBookField.setText(""); recommendationAuthorField.setText(""); recommendationPublisherField.setText("");
            if (recommendationCategoryBox.getItemCount() > 0) recommendationCategoryBox.setSelectedIndex(0);
            recommendationReasonField.setText("");
            loadMyRecommendations();
        });
    }

    private void showCopies(Book book) {
        if (book == null) { return; }
        load("正在查询实体副本…", () -> LibraryClientSrv.getBookCopies(book.getBookId()), copies -> {
            if (copyTable == null) return;
            copyModel.setRowCount(0);
            borrowableCopies.clear();
            copyTable.clearSelection();
            for (BookCopy c : copies) {
                copyModel.addRow(new Object[] { c.getCopyId(), c.getLocation(), c.getAcquiredDate(), copyStatus(c.getStatus()),
                        circulationText(c.getCirculationType()), c.getRemark() });
                if (c.isBorrowable()) {
                    borrowableCopies.add(c);
                }
            }
            int availableCount = borrowableCopies.size();
            // 自动刷新同一本书时，不先清掉热门提示再异步补回，避免标签每 3 秒闪烁一次。
            String baseLabel = bookLabelText(book, availableCount);
            if (selectedBookLabel.getText() == null
                    || !selectedBookLabel.getText().contains(" ·  " + book.getBookId() + "  · ")) {
                setSelectedBookLabelIfChanged(baseLabel);
            }
            refreshHotBookBadge(book, availableCount);
            LocalDate today = LocalDate.now();
            borrowDateLabel.setText(today.toString());
            dueDateLabel.setText("确认借阅时由系统计算：预约达到 3 人为 14 天，否则为 30 天");
        });
    }

    /** 仅使用当前预约队列实时展示热门标识，不需要在数据库额外保存热门字段。 */
    private void refreshHotBookBadge(Book book, int availableCount) {
        load("正在检查预约需求…", () -> LibraryClientSrv.getReservationQueue(book.getBookId()), queue -> {
            Book selected = selectedBook();
            // 用户已切换书目时，忽略前一本书较晚返回的网络结果。
            if (selected == null || !book.getBookId().equals(selected.getBookId()) || selectedBookLabel == null) return;
            String base = bookLabelText(book, availableCount);
            if (queue.size() >= 3) {
                setSelectedBookLabelIfChanged(base + "  ·  【热门图书：" + queue.size() + " 人预约，借期 14 天，不可续借】");
            } else if (!queue.isEmpty()) {
                setSelectedBookLabelIfChanged(base + "  ·  当前 " + queue.size() + " 人预约");
            } else {
                setSelectedBookLabelIfChanged(base);
            }
        });
    }

    private String bookLabelText(Book book, int availableCount) {
        return "《" + book.getBookName() + "》  ·  " + book.getBookId() + "  ·  可借副本 " + availableCount + " 本";
    }

    /** 只有文字实际变化时才重绘，保证实时刷新不造成标签闪烁。 */
    private void setSelectedBookLabelIfChanged(String text) {
        if (selectedBookLabel != null && !text.equals(selectedBookLabel.getText())) {
            selectedBookLabel.setText(text);
        }
    }

    private void borrowBook() {
        List<Book> selected = selectedBooks();
        if (selected.isEmpty()) { hint("请先选中要借阅的书目。"); return; }
        if (wouldExceedBorrowLimit(selected.size())) {
            return;
        }
        if (selected.size() > 1) {
            status("已选择 " + selected.size() + " 本书。请点击下方“确认借阅”；系统将为每本书自动分配首个可外借副本。", false);
            return;
        }
        Book book = selected.get(0);
        showCopies(book);
        status("请在下方借阅信息区选择实体副本后，点击“确认借阅”。", false);
    }

    private void confirmBorrow() {
        List<Book> selected = selectedBooks();
        if (selected.size() > 1) {
            confirmMultipleBorrow(selected);
            return;
        }
        if (wouldExceedBorrowLimit(1)) return;
        Book book = selected.isEmpty() ? null : selected.get(0);
        BookCopy copy = selectedBorrowableCopy();
        if (book == null || copy == null) {
            hint("当前没有可办理借阅的副本。请先选中书目和可外借副本；若该书没有空闲的可外借副本，请点击上方“预约”按钮排队。");
            return;
        }
        LocalDate today = LocalDate.now();
        action("正在提交借阅…", () -> LibraryClientSrv.borrowBook(copy.getCopyId(), readerId, today, today.plusDays(30)),
                () -> { loadBooks(); loadBorrowRecords(); });
    }

    /** 多选借阅时，系统为每个书目选择第一个可借实体副本；服务端仍逐本检查预约、禁借和总册数上限。 */
    private void confirmMultipleBorrow(List<Book> selected) {
        if (wouldExceedBorrowLimit(selected.size())) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "确认借阅选中的 " + selected.size() + " 本书吗？\n系统将为每本书自动分配首个可外借实体副本。",
                "确认批量借阅", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        status("正在办理批量借阅…", false);
        new SwingWorker<String, Void>() {
            protected String doInBackground() {
                int success = 0;
                List<String> failures = new ArrayList<>();
                LocalDate today = LocalDate.now();
                for (Book book : selected) {
                    BookCopy copy = null;
                    for (BookCopy candidate : LibraryClientSrv.getBookCopies(book.getBookId())) {
                        if (candidate.isBorrowable()) { copy = candidate; break; }
                    }
                    if (copy == null) {
                        failures.add("《" + book.getBookName() + "》暂无可外借副本");
                        continue;
                    }
                    Message response = LibraryClientSrv.borrowBook(copy.getCopyId(), readerId, today, today.plusDays(30));
                    if (response != null && response.isSuccess()) success++;
                    else failures.add("《" + book.getBookName() + "》" + (response == null ? "通信失败" : response.getResponseMsg()));
                }
                String message = "批量借阅完成：成功 " + success + " 本";
                return failures.isEmpty() ? message : message + "；未成功：" + String.join("；", failures);
            }
            protected void done() {
                try {
                    String message = get();
                    status(message, message.contains("未成功"));
                    JOptionPane.showMessageDialog(LibraryPanel.this, message, "批量借阅结果", JOptionPane.INFORMATION_MESSAGE);
                    loadBooks(); loadBorrowRecords();
                } catch (Exception e) { status("批量借阅通信失败：" + e.getMessage(), true); }
            }
        }.execute();
    }

    /** 借阅以副本表当前选中行作为唯一依据，避免下拉框自动选中造成误借。 */
    private BookCopy selectedBorrowableCopy() {
        if (copyTable == null || copyTable.getSelectedRow() < 0) return null;
        Object value = copyModel.getValueAt(copyTable.getSelectedRow(), 0);
        String copyId = value == null ? "" : String.valueOf(value);
        for (BookCopy copy : borrowableCopies) {
            if (copyId.equals(copy.getCopyId())) return copy;
        }
        return null;
    }

    private void reserveBook() {
        Book book = selectedBook();
        if (book == null) { hint("请先选中要预约的书目。"); return; }
        action("正在提交预约…", () -> LibraryClientSrv.reserveBook(book.getBookId(), readerId, LocalDateTime.now()), this::loadReservations);
    }

    private void returnBook() {
        BorrowRecord record = selectedBorrowRecord();
        if (record == null) { hint("请先选中借阅记录。"); return; }
        LocalDate date = LocalDate.now();
        action("正在提交归还…", () -> LibraryClientSrv.returnBook(record.getRecordId(), date), response -> {
            String notice = returnNotice(record, date);
            status(notice, false);
            JOptionPane.showMessageDialog(this, notice, "归还结果", JOptionPane.INFORMATION_MESSAGE);
            loadBorrowRecords(); loadBooks();
        });
    }

    private void returnDamagedBook() {
        BorrowRecord record = selectedBorrowRecord();
        if (record == null) { hint("请先选中要归还的借阅记录。 "); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
                "确认将《" + bookText(record.getBookName(), record.getBookId()) + "》归还并登记为损坏吗？\n"
                + "该副本将停止外借；系统将扣除 " + String.format("%.2f", DAMAGE_FINE)
                + " 元损坏处理费，且你的借阅权限将暂停 7 天。",
                "确认登记损坏", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        action("正在归还并登记损坏…", () -> LibraryClientSrv.returnDamagedBook(record.getRecordId(), LocalDate.now()), response -> {
            status("归还成功。副本已登记为损坏、停止外借；已扣除 " + String.format("%.2f", DAMAGE_FINE)
                    + " 元损坏处理费，借阅权限暂停 7 天。", false);
            loadBorrowRecords(); loadBooks();
        });
    }

    private void renewBook() {
        BorrowRecord record = selectedBorrowRecord();
        if (record == null) { hint("请先选中借阅记录。"); return; }
        LocalDate nextDue = record.getDueDate() == null ? LocalDate.now().plusDays(30) : record.getDueDate().plusDays(30);
        action("正在提交续借…", () -> LibraryClientSrv.renewBook(record.getRecordId(), nextDue), this::loadBorrowRecords);
    }

    private void cancelReservation() {
        ReservationRecord record = selectedReservation();
        if (record == null) { hint("请先选中预约记录。"); return; }
        action("正在取消预约…", () -> LibraryClientSrv.cancelReservation(record.getReservationId()), this::loadReservations);
    }

    private void borrowReadyReservation() {
        ReservationRecord reservation = selectedReservation();
        if (reservation == null) { hint("请先选中一条预约记录。"); return; }
        if (!reservation.isReady()) { hint("只有状态为“已到馆，可直接借阅”的预约才可在此办理借阅。"); return; }
        load("正在为预约图书分配副本…", () -> LibraryClientSrv.getBookCopies(reservation.getBookId()), copies -> {
            for (BookCopy copy : copies) {
                if (copy.isBorrowable()) {
                    LocalDate today = LocalDate.now();
                    action("正在办理预约借阅…", () -> LibraryClientSrv.borrowBook(copy.getCopyId(), readerId, today, today.plusDays(30)),
                            () -> { loadReservations(); loadBorrowRecords(); loadBooks(); });
                    return;
                }
            }
            hint("该预约图书暂时没有可借副本，请稍后刷新预约记录。");
        });
    }

    private void editBook(Book old) {
        if (old == null && !isAdmin) return;
        load("正在加载书目分类…", LibraryClientSrv::getAllBookCategories, categories -> editBookForm(old, categories));
    }

    private void editBookForm(Book old, List<BookCategory> categories) {
        if (categories.isEmpty()) { hint("暂无可选分类，请先建立图书分类。"); return; }
        JTextField id = new JTextField(old == null ? "" : old.getBookId());
        JTextField name = new JTextField(old == null ? "" : old.getBookName());
        JTextField author = new JTextField(old == null ? "" : old.getbAuthor());
        JTextField publisher = new JTextField(old == null ? "" : old.getbPublisher());
        JComboBox<String> category = new JComboBox<>();
        for (BookCategory item : categories) {
            category.addItem(item.getCategoryId() + " — " + item.getCategoryName());
            if (old != null && item.getCategoryId().equals(old.getCategoryId())) category.setSelectedIndex(category.getItemCount() - 1);
        }
        JTextField year = new JTextField(old == null || old.getPublishYear() == null ? "" : old.getPublishYear().toString());
        id.setEditable(old == null);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));
        field(form, "书目编号：", id); field(form, "书名：", name); field(form, "作者：", author); field(form, "出版社：", publisher); field(form, "分类编号：", category); field(form, "出版年份：", year);
        if (JOptionPane.showConfirmDialog(this, form, old == null ? "新增书目" : "修改书目", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            Integer publishYear = year.getText().trim().isEmpty() ? null : Integer.valueOf(year.getText().trim());
            Book book = new Book(id.getText().trim(), name.getText().trim(), author.getText().trim(), publisher.getText().trim(), categories.get(category.getSelectedIndex()).getCategoryId(), publishYear, old == null ? 0 : old.getbTotal(), old == null ? 0 : old.getbAvailable());
            action("正在保存书目…", () -> old == null ? LibraryClientSrv.addBook(book) : LibraryClientSrv.updateBook(book), this::loadBooks);
        } catch (NumberFormatException e) { hint("出版年份必须是数字。"); }
    }

    private void deleteBook() {
        Book book = selectedBook();
        if (book == null) { hint("请先选中书目。"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "确认删除《" + book.getBookName() + "》吗？\n仍有副本时系统会拒绝删除。", "确认删除", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) action("正在删除书目…", () -> LibraryClientSrv.deleteBook(book.getBookId()), this::loadBooks);
    }

    private void addCopy() {
        Book book = selectedBook();
        if (book == null) { hint("请先在书目表中选中书目。"); return; }
        load("正在加载已有馆藏位置…", () -> {
            java.util.Set<String> locations = new java.util.LinkedHashSet<>();
            for (Book existing : LibraryClientSrv.getAllBooks()) {
                for (BookCopy copy : LibraryClientSrv.getBookCopies(existing.getBookId())) {
                    if (copy.getLocation() != null && !copy.getLocation().trim().isEmpty()) locations.add(copy.getLocation().trim());
                }
            }
            locations.add("开架流通书库");
            locations.add("保存本阅览室");
            return new ArrayList<>(locations);
        }, locations -> addCopyForm(book, locations));
    }

    private void addCopyForm(Book book, List<String> locations) {
        JTextField id = new JTextField();
        JComboBox<String> location = new JComboBox<>(locations.toArray(new String[0]));
        JComboBox<String> circulation = new JComboBox<>(new String[] { "可外借", "仅馆内阅读" });
        JTextField date = new JTextField(LocalDate.now().toString()); JTextField remark = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        field(form, "副本编号：", id); field(form, "流通类型：", circulation); field(form, "馆藏位置：", location); field(form, "入库日期：", date); field(form, "备注：", remark);
        if (JOptionPane.showConfirmDialog(this, form, "新增实体副本", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            BookCopy copy = new BookCopy(id.getText().trim(), book.getBookId(), (String) location.getSelectedItem(), LocalDate.parse(date.getText().trim()));
            copy.setCirculationType(circulation.getSelectedIndex() == 0 ? BookCopy.CIRCULATION_CIRCULATING : BookCopy.CIRCULATION_IN_LIBRARY_ONLY);
            copy.setRemark(remark.getText().trim());
            action("正在新增副本…", () -> LibraryClientSrv.addBookCopy(copy), this::loadBooks);
        } catch (Exception e) { hint("入库日期格式应为 yyyy-MM-dd。"); }
    }

    private Book selectedBook() {
        if (bookTable == null || bookTable.getSelectedRow() < 0) return null;
        int row = bookTable.convertRowIndexToModel(bookTable.getSelectedRow());
        return row >= 0 && row < books.size() ? books.get(row) : null;
    }

    private List<Book> selectedBooks() {
        List<Book> selected = new ArrayList<>();
        if (bookTable == null) return selected;
        for (int row : bookTable.getSelectedRows()) {
            int modelRow = bookTable.convertRowIndexToModel(row);
            if (modelRow >= 0 && modelRow < books.size()) selected.add(books.get(modelRow));
        }
        return selected;
    }

    /** 前端先明确提示“当前已借满”，服务端仍会再次校验，防止并发借阅突破上限。 */
    private boolean wouldExceedBorrowLimit(int plannedBorrowCount) {
        int activeCount = 0;
        for (BorrowRecord record : borrowRecords) {
            if (BorrowRecord.STATUS_BORROWED.equals(record.getStatus())
                    || BorrowRecord.STATUS_OVERDUE.equals(record.getStatus())) activeCount++;
        }
        if (activeCount >= MAX_BORROWED_BOOKS) {
            hint("你当前已借阅 " + activeCount + " 本图书，已达到同时借阅 "
                    + MAX_BORROWED_BOOKS + " 本的上限，请先归还后再借阅。");
            return true;
        }
        if (activeCount + plannedBorrowCount > MAX_BORROWED_BOOKS) {
            hint("你当前已借阅 " + activeCount + " 本，本次选择 " + plannedBorrowCount
                    + " 本后将超过同时借阅 " + MAX_BORROWED_BOOKS + " 本的上限，请减少选择数量。 ");
            return true;
        }
        return false;
    }

    private BorrowRecord selectedBorrowRecord() {
        if (borrowTable == null || borrowTable.getSelectedRow() < 0) return null;
        int row = borrowTable.convertRowIndexToModel(borrowTable.getSelectedRow());
        return row >= 0 && row < borrowRecords.size() ? borrowRecords.get(row) : null;
    }
    private ReservationRecord selectedReservation() {
        if (reservationTable == null || reservationTable.getSelectedRow() < 0) return null;
        int row = reservationTable.convertRowIndexToModel(reservationTable.getSelectedRow());
        return row >= 0 && row < reservations.size() ? reservations.get(row) : null;
    }

    private <T> void load(String progress, Supplier<List<T>> request, Consumer<List<T>> success) {
        load(progress, request, success, null);
    }
    private <T> void load(String progress, Supplier<List<T>> request, Consumer<List<T>> success, Runnable finished) {
        status(progress, false);
        new SwingWorker<List<T>, Void>() {
            protected List<T> doInBackground() { return request.get(); }
            protected void done() {
                try {
                    success.accept(get());
                } catch (Exception e) {
                    status("通信失败：" + e.getMessage(), true);
                } finally {
                    if (finished != null) finished.run();
                }
            }
        }.execute();
    }
    private void action(String progress, Supplier<Message> request, Runnable success) {
        action(progress, request, response -> {
            JOptionPane.showMessageDialog(LibraryPanel.this, response.getResponseMsg(), "图书馆", JOptionPane.INFORMATION_MESSAGE);
            success.run();
        });
    }
    private void action(String progress, Supplier<Message> request, Consumer<Message> success) {
        status(progress, false);
        new SwingWorker<Message, Void>() {
            protected Message doInBackground() { return request.get(); }
            protected void done() {
                try {
                    Message response = get();
                    if (response != null && response.isSuccess()) { status(response.getResponseMsg(), false); success.accept(response); }
                    else { String message = response == null ? "服务器没有返回结果" : response.getResponseMsg(); status(message, true); JOptionPane.showMessageDialog(LibraryPanel.this, message, "操作失败", JOptionPane.WARNING_MESSAGE); }
                } catch (Exception e) { status("通信失败：" + e.getMessage(), true); }
            }
        }.execute();
    }

    private JTextArea helpText(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(font(Font.PLAIN, 12));
        area.setForeground(new Color(90, 105, 120));
        area.setBorder(new EmptyBorder(8, 12, 8, 12));
        return area;
    }

    private JPanel basePanel() { JPanel p = new JPanel(new BorderLayout(8, 8)); p.setBackground(BACKGROUND); p.setBorder(new EmptyBorder(12, 14,12, 14)); return p; }
    private JPanel toolPanel() { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(10, 12, 10, 12)); return p; }
    private DefaultTableModel model(String[] names) { return new DefaultTableModel(names, 0) { private static final long serialVersionUID = 1L; public boolean isCellEditable(int r, int c) { return false; } }; }
    private void configureTable(JTable table) { table.setFont(font(Font.PLAIN, 13)); table.setRowHeight(27); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.getTableHeader().setFont(font(Font.BOLD, 13)); table.getTableHeader().setBackground(new Color(225, 239, 249)); table.setGridColor(new Color(224, 230, 235)); }
    private JButton button(String text, Color color) { JButton b = new JButton(text); b.setFont(font(Font.PLAIN, 13)); b.setBackground(color); b.setForeground(Color.WHITE); b.setFocusPainted(false); b.setBorder(new EmptyBorder(6, 12, 6, 12)); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); return b; }
    private JLabel label(String text) { JLabel l = new JLabel(text); l.setFont(font(Font.BOLD, 13)); return l; }
    private void field(JPanel panel, String name, java.awt.Component component) { panel.add(label(name)); panel.add(component); }
    private Font font(int style, float size) { Font f = javax.swing.UIManager.getFont("defaultFont"); return f == null ? new Font("Microsoft YaHei", style, (int) size) : f.deriveFont(style, size); }
    private void status(String text, boolean error) { if (statusLabel != null) { statusLabel.setText(text == null ? "" : text); statusLabel.setForeground(error ? new Color(180, 65, 60) : new Color(90, 105, 120)); } }
    private void hint(String text) { JOptionPane.showMessageDialog(this, text, "提示", JOptionPane.INFORMATION_MESSAGE); }
    private String bookText(String bookName, String bookId) { return (bookName == null || bookName.trim().isEmpty()) ? safe(bookId) : bookName + "（" + safe(bookId) + "）"; }
    private String displayId(String id, String prefix, int sequence) { return id != null && id.matches(prefix + "\\d+") ? id : prefix + String.format("%04d", sequence); }
    private String borrowStatus(String status) { if (BorrowRecord.STATUS_BORROWED.equals(status)) return "在借中"; if (BorrowRecord.STATUS_RETURNED.equals(status)) return "已归还"; if (BorrowRecord.STATUS_OVERDUE.equals(status)) return "已逾期"; return safe(status); }
    private String reservationStatus(String status) { if (ReservationRecord.STATUS_WAITING.equals(status)) return "排队中"; if (ReservationRecord.STATUS_READY.equals(status)) return "已到馆，请在 7 天内借阅"; if (ReservationRecord.STATUS_COMPLETED.equals(status)) return "已完成"; if (ReservationRecord.STATUS_CANCELLED.equals(status)) return "已取消"; if (ReservationRecord.STATUS_EXPIRED.equals(status)) return "保留期已过，预约失效"; return safe(status); }
    private String recommendationStatus(String status) { return BookRecommendation.STATUS_PENDING.equals(status) ? "待采购评估" : safe(status); }
    private String reservationTimeText(LocalDateTime time) { return time == null ? "—" : DATE_TIME_FORMAT.format(time); }
    private String readyDeadlineText(ReservationRecord record) { return record == null || !record.isReady() || record.getReadyTime() == null ? "—" : DATE_TIME_FORMAT.format(record.getReadyTime().plusDays(7)); }
    private String copyStatus(String status) { return BookCopy.STATUS_AVAILABLE.equals(status) ? "可借" : BookCopy.STATUS_BORROWED.equals(status) ? "借出" : BookCopy.STATUS_DAMAGED.equals(status) ? "损坏，不可外借" : BookCopy.STATUS_LOST.equals(status) ? "遗失" : safe(status); }
    private String circulationText(String type) { return BookCopy.CIRCULATION_IN_LIBRARY_ONLY.equals(type) ? "仅馆内阅读" : "可外借"; }
    private String categoryText(String categoryId) {
        for (Map.Entry<String, String> entry : categoryIds.entrySet()) if (entry.getValue().equals(categoryId)) return entry.getKey();
        return safe(categoryId);
    }
    private String safe(String text) { return text == null ? "" : text; }
    private String returnNotice(BorrowRecord record, LocalDate returnDate) {
        if (record.getDueDate() == null || !returnDate.isAfter(record.getDueDate())) return "归还成功。《" + bookText(record.getBookName(), record.getBookId()) + "》已正常归还。";
        long days = java.time.temporal.ChronoUnit.DAYS.between(record.getDueDate(), returnDate);
        double fine = days * OVERDUE_FINE_PER_DAY;
        return "归还成功，但《" + bookText(record.getBookName(), record.getBookId()) + "》逾期 " + days + " 天。"
                + "已从校园一卡通余额扣除逾期占用费 " + String.format("%.2f", fine) + " 元。";
    }
    private String readerIdOf(Object user) { if (user instanceof Student) { Student s = (Student) user; return usable(s.getSCard(), s.getSId()); } if (user instanceof Teacher) { Teacher t = (Teacher) user; return usable(t.getCardNumber(), t.getTeacherId()); } return null; }
    private String usable(String preferred, String fallback) { return preferred == null || preferred.trim().isEmpty() ? fallback : preferred; }

    /** 主界面上的一个检索条件标签，例如“作者：东野圭吾”。 */
    private static final class SearchTag {
        private final String field;
        private final String keyword;

        private SearchTag(String field, String keyword) {
            this.field = field;
            this.keyword = keyword;
        }
    }
}
