package vCampus.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import vCampus.common.Message;
import vCampus.common.vo.Admin;
import vCampus.common.vo.BorrowRecord;
import vCampus.common.vo.LibraryAdminRequest;

/** 管理员查询全馆借阅历史，并按读者一卡通号代办借还。 */
public final class LibraryAdminPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color PAGE = new Color(244, 248, 252);
    private static final Color BLUE = new Color(48, 121, 181);
    private static final Color GREEN = new Color(55, 148, 106);
    private static final Color ORANGE = new Color(221, 139, 34);
    private static final Color CARD_BORDER = new Color(211, 224, 235);

    private final Admin admin;
    private final Runnable changed;
    private final JTextField readerFilter = new JTextField();
    private final JTextField bookFilter = new JTextField();
    private final JTextField fromFilter = new JTextField();
    private final JTextField toFilter = new JTextField();
    private final JTextField operationReader = new JTextField();
    private final JTextField operationCopy = new JTextField();
    private final JLabel notice = new JLabel("进入页面后自动查询全部借阅记录。", SwingConstants.LEFT);
    private final JButton borrow = new JButton("确认代借");
    private final JButton returnBook = new JButton("确认归还");
    private final List<BorrowRecord> records = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(new String[] {
            "记录编号", "用户 ID（一卡通号）", "书名", "副本编号", "借阅日期", "应还日期", "归还日期", "状态" }, 0) {
        private static final long serialVersionUID = 1L;
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private long queryVersion;
    private boolean operating;
    private boolean refreshing;
    /** 管理员当前查看记录时，每 3 秒静默同步一次。 */
    private final javax.swing.Timer liveRefreshTimer;

    public LibraryAdminPanel(Admin admin, Runnable changed) {
        super(new BorderLayout(12, 12));
        this.admin = admin;
        this.changed = changed;
        setBackground(PAGE);
        setBorder(new EmptyBorder(14, 16, 14, 16));

        add(header(), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(queryCard(), BorderLayout.NORTH);
        center.add(recordCard(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(operationCard(), BorderLayout.SOUTH);

        borrow.addActionListener(e -> operate(false));
        returnBook.addActionListener(e -> operate(true));
        for (JTextField input : new JTextField[] { readerFilter, bookFilter, fromFilter, toFilter }) input.addActionListener(e -> refresh());
        liveRefreshTimer = new javax.swing.Timer(3000, e -> refresh(true));
        liveRefreshTimer.setCoalesce(true);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (isShowing()) {
                refresh();
                liveRefreshTimer.start();
            } else {
                liveRefreshTimer.stop();
            }
        });
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("借阅管理");
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 22));
        title.setForeground(new Color(39, 78, 112));
        header.add(title, BorderLayout.WEST);
        JLabel subtitle = new JLabel("全馆记录查询 · 管理员代借代还");
        subtitle.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        subtitle.setForeground(new Color(100, 119, 136));
        header.add(subtitle, BorderLayout.EAST);
        return header;
    }

    private JPanel queryCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(10, 8));
        card.add(sectionTitle("查询借阅记录", "日期按借阅日期筛选，开始日和结束日均包含在范围内。"), BorderLayout.NORTH);
        JPanel filters = new JPanel(new GridLayout(2, 2, 12, 8));
        filters.setOpaque(false);
        field(filters, "用户 ID（一卡通号，精确匹配）", readerFilter);
        field(filters, "书名（包含关键词）", bookFilter);
        field(filters, "借阅开始日期（yyyy-MM-dd）", fromFilter);
        field(filters, "借阅结束日期（yyyy-MM-dd，含当天）", toFilter);
        card.add(filters, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton reset = actionButton("清空条件", new Color(104, 126, 146));
        JButton search = actionButton("查询记录", BLUE);
        reset.addActionListener(e -> { readerFilter.setText(""); bookFilter.setText(""); fromFilter.setText(""); toFilter.setText(""); refresh(); });
        search.addActionListener(e -> refresh());
        actions.add(reset);
        actions.add(search);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel recordCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 8));
        card.add(sectionTitle("借阅记录", "点击一条记录后，会自动带入下方代办区域。"), BorderLayout.NORTH);
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        table.setRowHeight(29);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setGridColor(new Color(229, 235, 240));
        table.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(225, 239, 249));
        for (int i = 0; i < model.getColumnCount(); i++) table.getColumnModel().getColumn(i).setPreferredWidth(i == 2 ? 220 : 135);
        table.getSelectionModel().addListSelectionListener(e -> {
            BorrowRecord selected = selectedRecord();
            if (!e.getValueIsAdjusting() && selected != null && !operating) {
                operationReader.setText(selected.getReaderId());
                operationCopy.setText(selected.getCopyId());
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel operationCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(10, 8));
        card.add(sectionTitle("管理员代办", "代借需填写用户 ID 和可外借副本；归还前请先查询并选中该用户的在借记录。"), BorderLayout.NORTH);
        JPanel form = new JPanel(new GridLayout(1, 2, 12, 0));
        form.setOpaque(false);
        field(form, "代办用户 ID（一卡通号）", operationReader);
        field(form, "借书副本编号（例如 CP001）", operationCopy);
        card.add(form, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        notice.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        notice.setForeground(new Color(91, 108, 123));
        bottom.add(notice, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        styleButton(borrow, GREEN);
        styleButton(returnBook, ORANGE);
        actions.add(borrow);
        actions.add(returnBook);
        bottom.add(actions, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel card() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(CARD_BORDER), new EmptyBorder(10, 12, 10, 12)));
        return card;
    }

    private JPanel sectionTitle(String title, String description) {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        JLabel left = new JLabel(title);
        left.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        left.setForeground(new Color(43, 82, 116));
        JLabel right = new JLabel(description, SwingConstants.RIGHT);
        right.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        right.setForeground(new Color(104, 121, 136));
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.CENTER);
        return header;
    }

    private void field(JPanel panel, String label, JTextField input) {
        JPanel field = new JPanel(new BorderLayout(0, 4));
        field.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        name.setForeground(new Color(72, 92, 110));
        input.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        field.add(name, BorderLayout.NORTH);
        field.add(input, BorderLayout.CENTER);
        panel.add(field);
    }

    private JButton actionButton(String text, Color color) {
        JButton button = new JButton(text);
        styleButton(button, color);
        return button;
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(7, 14, 7, 14));
    }

    private LibraryAdminRequest request() {
        LibraryAdminRequest request = new LibraryAdminRequest();
        request.sessionToken = admin.getLibrarySessionToken();
        return request;
    }

    private LocalDate date(JTextField field) {
        return field.getText().trim().isEmpty() ? null : LocalDate.parse(field.getText().trim());
    }

    private void refresh() {
        refresh(false);
    }

    /** 自动刷新只更新表格，不对尚未填完的筛选日期反复弹出提示。 */
    private void refresh(boolean automatic) {
        if (refreshing) return;
        LibraryAdminRequest query = request();
        query.readerId = readerFilter.getText().trim();
        query.bookName = bookFilter.getText().trim();
        try {
            query.fromDate = date(fromFilter);
            query.toDate = date(toFilter);
            if (query.fromDate != null && query.toDate != null && query.fromDate.isAfter(query.toDate)) {
                throw new IllegalArgumentException("借阅开始日期不能晚于结束日期");
            }
        } catch (DateTimeParseException e) {
            if (!automatic) hint("日期请按 yyyy-MM-dd 填写，或留空。");
            else notice.setText("筛选日期尚未填写完整，已暂停自动刷新。");
            return;
        } catch (IllegalArgumentException e) {
            if (!automatic) hint(e.getMessage());
            else notice.setText(e.getMessage() + "，已暂停自动刷新。");
            return;
        }
        refreshing = true;
        long version = ++queryVersion;
        notice.setText("正在按借阅日期查询记录…");
        new SwingWorker<Message, Void>() {
            protected Message doInBackground() { return LibraryClientSrv.queryAdminBorrowRecords(query); }
            protected void done() {
                if (version != queryVersion) return;
                try {
                    Message response = get();
                    if (response == null || !response.isSuccess()) {
                        notice.setText(response == null ? "未收到服务器响应" : response.getResponseMsg());
                        return;
                    }
                    if (!(response.getData() instanceof List<?>)) throw new IllegalStateException("服务器返回的记录格式不正确");
                    records.clear();
                    model.setRowCount(0);
                    for (Object value : (List<?>) response.getData()) {
                        BorrowRecord record = (BorrowRecord) value;
                        records.add(record);
                        String state = BorrowRecord.STATUS_RETURNED.equals(record.getStatus()) ? "已归还"
                                : record.isOverdue(LocalDate.now()) ? "已逾期" : "在借中";
                        model.addRow(new Object[] { record.getRecordId(), record.getReaderId(), record.getBookName(), record.getCopyId(),
                                record.getBorrowDate(), record.getDueDate(), record.getReturnDate(), state });
                    }
                    notice.setText("查询完成：共 " + records.size() + " 条记录（包括已归还历史）。");
                } catch (Exception e) {
                    notice.setText("查询失败：" + e.getMessage());
                } finally {
                    refreshing = false;
                }
            }
        }.execute();
    }

    private BorrowRecord selectedRecord() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int index = table.convertRowIndexToModel(row);
        return index < records.size() ? records.get(index) : null;
    }

    private void operate(boolean returning) {
        if (operating) return;
        LibraryAdminRequest operation = request();
        operation.readerId = operationReader.getText().trim();
        operation.copyId = operationCopy.getText().trim();
        if (operation.readerId.isEmpty()) { hint("请填写代办用户 ID（一卡通号）。"); return; }
        BorrowRecord record = selectedRecord();
        String details;
        if (returning) {
            if (record == null) { hint("请先选中要归还的借阅记录。"); return; }
            if (!operation.readerId.equals(record.getReaderId())) { hint("用户 ID 与选中记录不一致，请核对。"); return; }
            if (!BorrowRecord.STATUS_BORROWED.equals(record.getStatus()) && !BorrowRecord.STATUS_OVERDUE.equals(record.getStatus())) {
                hint("这条记录已归还，不能重复归还。");
                return;
            }
            operation.recordId = record.getRecordId();
            details = "归还《" + record.getBookName() + "》，副本 " + record.getCopyId() + "。\n逾期费用按规则从该用户一卡通扣除。";
        } else {
            if (operation.copyId.isEmpty()) { hint("请填写可外借的实体副本编号。"); return; }
            details = "借阅副本 " + operation.copyId + "，借期 30 天，应还日期 " + LocalDate.now().plusDays(30) + "。";
        }
        if (JOptionPane.showConfirmDialog(this, "确认替用户 " + operation.readerId + "\n" + details,
                "确认代办", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        operating = true;
        borrow.setEnabled(false);
        returnBook.setEnabled(false);
        new SwingWorker<Message, Void>() {
            protected Message doInBackground() { return returning ? LibraryClientSrv.adminReturn(operation) : LibraryClientSrv.adminBorrow(operation); }
            protected void done() {
                try {
                    Message response = get();
                    if (response == null || !response.isSuccess()) {
                        hint(response == null ? "未收到服务器响应，请查询记录确认后再操作。" : response.getResponseMsg());
                        return;
                    }
                    hint(response.getResponseMsg());
                    readerFilter.setText(operation.readerId);
                    bookFilter.setText("");
                    fromFilter.setText("");
                    toFilter.setText("");
                    refresh();
                    changed.run();
                } catch (Exception e) {
                    hint("代办响应失败，请查询记录确认结果：" + e.getMessage());
                } finally {
                    operating = false;
                    borrow.setEnabled(true);
                    returnBook.setEnabled(true);
                }
            }
        }.execute();
    }

    private void hint(String text) { JOptionPane.showMessageDialog(this, text, "图书馆借阅管理", JOptionPane.INFORMATION_MESSAGE); }
}
