package vCampus.client;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import vCampus.common.Message;
import vCampus.common.vo.Admin;
import vCampus.common.vo.BankAccount;
import vCampus.common.vo.BankTransaction;
import vCampus.common.vo.Student;
import vCampus.common.vo.Teacher;

public class BankPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Color BACKGROUND = new Color(248, 250, 252);
	private static final Color HEADER_RED = new Color(201, 105, 105);
	private static final Color OPEN_RED = new Color(207, 123, 123);
	private static final Color DEPOSIT_GREEN = new Color(76, 157, 108);
	private static final Color WITHDRAW_ORANGE = new Color(222, 151, 72);
	private static final Color TRANSFER_BLUE = new Color(78, 133, 180);
	private static final Color RECHARGE_PURPLE = new Color(139, 119, 174);
	private static final Color CARD_BORDER = new Color(226, 226, 226);

	private final Object currentUser;
	private final String holderCard;
	private final String holderName;

	private JLabel cardLabel;
	private JLabel balanceLabel;
	private JLabel statusLabel;

	private JTable txTable;
	private DefaultTableModel txModel;

	private JTable adminTable;
	private DefaultTableModel adminModel;
	private JTextField adminSearchField;
	private java.util.List<BankAccount> adminAccounts;

	// 每5秒自动刷新一次银行账户信息
	private Timer autoRefreshTimer;
	private boolean refreshRunning = false;

	public BankPanel(Object user) {
		currentUser = user;
		holderCard = cardOf(user);
		holderName = nameOf(user);

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		setBackground(BACKGROUND);

		add(header(), BorderLayout.NORTH);
		add(buildTabs(), BorderLayout.CENTER);

		refresh();

		// Bank 页面打开期间，每 5 秒从服务器重新读取一次最新数据。
		autoRefreshTimer = new Timer(5000, e -> refreshAsync());
		autoRefreshTimer.setRepeats(true);

		// 页面显示时启动定时刷新，离开页面时停止，避免后台无意义请求。
		addHierarchyListener(e -> {
			if ((e.getChangeFlags()
					& java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
				if (isShowing()) {
					if (!autoRefreshTimer.isRunning()) {
						autoRefreshTimer.start();
					}
				} else {
					if (autoRefreshTimer.isRunning()) {
						autoRefreshTimer.stop();
					}
				}
			}
		});
	}

	private JPanel header() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(HEADER_RED);
		p.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

		JLabel title = new JLabel("校园银行");
		title.setForeground(Color.WHITE);
		title.setFont(new Font("微软雅黑", Font.BOLD, 24));
		p.add(title, BorderLayout.WEST);

		JLabel user = new JLabel("持卡人：" + holderName);
		user.setForeground(new Color(255, 245, 245));
		user.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		p.add(user, BorderLayout.EAST);

		return p;
	}

	private JTabbedPane buildTabs() {
		JTabbedPane tabs = new JTabbedPane();
		tabs.setFont(new Font("微软雅黑", Font.BOLD, 14));

		tabs.addTab("我的账户", accountPanel());

		if (currentUser instanceof Admin && ((Admin) currentUser).isManageBank()) {
			tabs.addTab("银行管理员", adminPanel());
		}

		return tabs;
	}

	private JPanel accountPanel() {
		JPanel p = new JPanel(new BorderLayout(12, 12));
		p.setBackground(BACKGROUND);
		p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

		// 账户信息区
		JPanel accountInfo = new JPanel(new GridLayout(3, 2, 20, 8));
		accountInfo.setBackground(Color.WHITE);
		accountInfo.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(CARD_BORDER),
				BorderFactory.createEmptyBorder(12, 16, 12, 16)));

		cardLabel = infoLabel("银行卡号：加载中");
		balanceLabel = infoLabel("余额：加载中");
		JLabel holderLabel = infoLabel("持卡人：" + holderName);
		JLabel campusCardLabel = infoLabel("一卡通号：" + holderCard);
		statusLabel = infoLabel("状态：加载中");

		accountInfo.add(cardLabel);
		accountInfo.add(balanceLabel);
		accountInfo.add(holderLabel);
		accountInfo.add(campusCardLabel);
		accountInfo.add(statusLabel);

		p.add(accountInfo, BorderLayout.NORTH);

		// 银行操作区
		JPanel operations = new JPanel(new BorderLayout());
		operations.setOpaque(false);
		operations.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));

		JLabel operationTitle = new JLabel("银行服务");
		operationTitle.setFont(new Font("微软雅黑", Font.BOLD, 16));
		operationTitle.setForeground(new Color(70, 70, 70));
		operationTitle.setBorder(BorderFactory.createEmptyBorder(0, 4, 12, 0));

		operations.add(operationTitle, BorderLayout.NORTH);

		JPanel buttonRow = new JPanel(new GridLayout(1, 5, 12, 0));
		buttonRow.setOpaque(false);

		buttonRow.add(bigButton("开户", OPEN_RED, e -> openAccount()));
		buttonRow.add(bigButton("存钱", DEPOSIT_GREEN, e -> amountAction("存钱")));
		buttonRow.add(bigButton("取钱", WITHDRAW_ORANGE, e -> amountAction("取钱")));
		buttonRow.add(bigButton("转账", TRANSFER_BLUE, e -> transfer()));
		buttonRow.add(bigButton("一卡通充值", RECHARGE_PURPLE, e -> amountAction("一卡通充值")));

		operations.add(buttonRow, BorderLayout.CENTER);
		p.add(operations, BorderLayout.CENTER);

		// 交易流水
		JPanel recordPanel = new JPanel(new BorderLayout());
		recordPanel.setBackground(Color.WHITE);
		recordPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(CARD_BORDER),
				BorderFactory.createEmptyBorder(8, 10, 10, 10)));

		JLabel recordTitle = new JLabel("交易明细");
		recordTitle.setFont(new Font("微软雅黑", Font.BOLD, 14));
		recordTitle.setForeground(new Color(80, 80, 80));
		recordTitle.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0));

		recordPanel.add(recordTitle, BorderLayout.NORTH);

		txModel = new DefaultTableModel(
				new String[] { "时间", "类型", "金额", "对方/一卡通", "余额", "说明" }, 0) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};

		txTable = new JTable(txModel);
		configureTransactionTable(txTable);

		JScrollPane scrollPane = new JScrollPane(txTable);
		scrollPane.setPreferredSize(new Dimension(0, 190));

		recordPanel.add(scrollPane, BorderLayout.CENTER);
		p.add(recordPanel, BorderLayout.SOUTH);

		return p;
	}

	private JLabel infoLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		label.setForeground(new Color(65, 65, 65));
		return label;
	}

	private void configureTransactionTable(JTable table) {
		table.setRowHeight(28);
		table.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 12));
		table.getTableHeader().setPreferredSize(new Dimension(0, 30));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFillsViewportHeight(true);

		// 只有交易明细表的第3列是金额
		if (table.getColumnCount() > 2) {
			table.getColumnModel().getColumn(2)
					.setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {

						private static final long serialVersionUID = 1L;

						@Override
						public Component getTableCellRendererComponent(
								JTable table,
								Object value,
								boolean isSelected,
								boolean hasFocus,
								int row,
								int column) {

							Component c = super.getTableCellRendererComponent(
									table, value, isSelected, hasFocus, row, column);

							if (value != null) {
								String text = value.toString();

								if (text.startsWith("-")) {
									c.setForeground(Color.RED);
								} else if (text.startsWith("+")) {
									c.setForeground(new Color(0, 150, 0));
								} else {
									c.setForeground(Color.BLACK);
								}
							}

							return c;
						}
					});
		}
	}

	private void configureAdminStatusColumn(JTable table) {
		table.getColumnModel().getColumn(4)
				.setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {

					private static final long serialVersionUID = 1L;

					@Override
					public Component getTableCellRendererComponent(
							JTable table,
							Object value,
							boolean isSelected,
							boolean hasFocus,
							int row,
							int column) {

						Component c = super.getTableCellRendererComponent(
								table, value, isSelected, hasFocus, row, column);

						if (value != null) {
							String status = value.toString();

							if ("正常".equals(status)) {
								c.setForeground(new Color(0, 150, 0));
							} else if ("冻结".equals(status)) {
								c.setForeground(Color.RED);
							} else {
								c.setForeground(Color.BLACK);
							}
						}

						return c;
					}
				});
	}

	private JPanel adminPanel() {
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.setBackground(Color.WHITE);
		p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		// 搜索区域
		JPanel top = new JPanel(new BorderLayout());
		top.setBackground(Color.WHITE);

		adminSearchField = new JTextField();
		adminSearchField.setPreferredSize(new Dimension(220, 32));

		JButton searchButton = new JButton("搜索");

		JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		searchPanel.setBackground(Color.WHITE);

		searchPanel.add(new JLabel("搜索: "));
		searchPanel.add(adminSearchField);
		searchPanel.add(searchButton);

		top.add(searchPanel, BorderLayout.EAST);
		p.add(top, BorderLayout.NORTH);

		// 管理员账户表
		adminModel = new DefaultTableModel(
				new String[] { "银行卡号", "持卡人", "一卡通号", "余额", "状态", "操作" }, 0) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};

		adminTable = new JTable(adminModel);

		adminTable.setRowHeight(30);
		adminTable.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		adminTable.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 12));
		adminTable.getTableHeader().setPreferredSize(new Dimension(0, 30));
		adminTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		adminTable.setFillsViewportHeight(true);

		configureAdminStatusColumn(adminTable);

		// 查看交易明细按钮
		adminTable.getColumnModel().getColumn(5)
        .setCellRenderer(new ButtonRenderer());

		adminTable.addMouseListener(new java.awt.event.MouseAdapter() {

			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {

				int row = adminTable.rowAtPoint(e.getPoint());
				int column = adminTable.columnAtPoint(e.getPoint());

				// 第6列 = 查看交易明细
				if (row >= 0 && column == 5) {

					String bankCardId =
							String.valueOf(adminModel.getValueAt(row, 0));

					String holderName =
							String.valueOf(adminModel.getValueAt(row, 1));

					String holderCard =
							String.valueOf(adminModel.getValueAt(row, 2));

					showAdminTransactions(
							bankCardId,
							holderName,
							holderCard);
				}
			}
		});

		adminTable.getColumnModel().getColumn(5)
				.setPreferredWidth(110);

		adminTable.getColumnModel().getColumn(5)
				.setMaxWidth(110);

		p.add(new JScrollPane(adminTable), BorderLayout.CENTER);

		// 冻结/解除冻结
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
		bar.setBackground(Color.WHITE);

		bar.add(bigButton(
				"冻结账户",
				new Color(194, 91, 91),
				e -> changeAdminStatus(0)));

		bar.add(bigButton(
				"解除冻结",
				DEPOSIT_GREEN,
				e -> changeAdminStatus(1)));

		p.add(bar, BorderLayout.SOUTH);

		searchButton.addActionListener(e -> filterAdminTable());
		adminSearchField.addActionListener(e -> filterAdminTable());

		return p;
	}

	private JPanel createPINPanel(
			JPasswordField[] fields,
			JButton showButton) {
		JPanel panel = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 3, 0));

		for (int i = 0; i < 6; i++) {
			JPasswordField field = new JPasswordField(1);
			field.setHorizontalAlignment(JPasswordField.CENTER);
			field.setFont(new Font("SansSerif", Font.BOLD, 18));
			final int index = i;
			field.addKeyListener(new java.awt.event.KeyAdapter() {
				@Override
				public void keyTyped(java.awt.event.KeyEvent e) {
					char c = e.getKeyChar();
					// 只能输入数字
					if (!Character.isDigit(c)) {
						e.consume();
						return;
					}
					// 每个格子只能有一个数字
					if (field.getPassword().length > 0) {
						e.consume();
						return;
					}
					// 输入后自动跳到下一个格子
					if (index < 5) {
						SwingUtilities.invokeLater(
								() -> fields[index + 1].requestFocusInWindow());
					}
				}

				@Override
				public void keyPressed(java.awt.event.KeyEvent e) {

					// 当前格为空时按退格，回到上一个格子
					if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE
							&& field.getPassword().length == 0
							&& index > 0) {
						fields[index - 1].requestFocusInWindow();
					}
				}
			});
			fields[i] = field;
			panel.add(field);
		}

		// 显示/隐藏密码
		showButton.addActionListener(e -> {
			boolean show = showButton.getText().equals("显示");
			for (JPasswordField field : fields) {
				field.setEchoChar(show ? (char) 0 : '•');
			}
			showButton.setText(show ? "隐藏" : "显示");
		});
		panel.add(showButton);
		return panel;
	}

	private String getPIN(JPasswordField[] fields) {
		StringBuilder pin = new StringBuilder();
		for (JPasswordField field : fields) {
			pin.append(field.getPassword());
		}
		return pin.toString();
	}

	private JButton bigButton(
			String text,
			Color background,
			java.awt.event.ActionListener action) {
		JButton b = new JButton(text);
		b.setPreferredSize(new Dimension(145, 58));
		b.setMinimumSize(new Dimension(120, 52));
		b.setFont(new Font("微软雅黑", Font.BOLD, 15));
		b.setForeground(Color.WHITE);
		b.setBackground(background);
		b.setOpaque(true);
		b.setFocusPainted(false);
		b.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		b.setCursor(new Cursor(Cursor.HAND_CURSOR));
		b.addActionListener(action);
		return b;
	}

	private void openAccount() {
		JPasswordField[] pinFields = new JPasswordField[6];
		JPasswordField[] confirmFields = new JPasswordField[6];
		JButton showPinButton = new JButton("显示");
		JButton showConfirmButton = new JButton("显示");
		JPanel pinRow = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 5, 3));
		pinRow.add(new JLabel("设置银行卡密码:"));
		pinRow.add(createPINPanel(pinFields, showPinButton));
		JPanel confirmRow = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 5, 3));
		confirmRow.add(new JLabel("确认银行卡密码:"));
		confirmRow.add(createPINPanel(confirmFields, showConfirmButton));
		JPanel content = new JPanel(
				new GridLayout(2, 1, 5, 8));
		content.add(pinRow);
		content.add(confirmRow);

		int result = JOptionPane.showConfirmDialog(
				this,
				content,
				"银行卡开户",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION) {
			return;
		}

		String pin = getPIN(pinFields);
		String confirm = getPIN(confirmFields);
		// 必须是完整6位数字
		if (!pin.matches("\\d{6}")) {
			JOptionPane.showMessageDialog(
					this,
					"请输入完整的6位银行卡密码。");
			return;
		}
		// 不允许000000
		if ("000000".equals(pin)) {
			JOptionPane.showMessageDialog(
					this,
					"银行卡密码不能设置为000000。");
			return;
		}
		// 确认密码也必须完整
		if (!confirm.matches("\\d{6}")) {
			JOptionPane.showMessageDialog(
					this,
					"请完整输入确认密码。");
			return;
		}
		// 两次密码必须一致
		if (!pin.equals(confirm)) {
			JOptionPane.showMessageDialog(
					this,
					"两次输入的银行卡密码不一致。");
			return;
		}
		BankAccount a =
				BankClientSrv.openAccount(
						holderCard,
						holderName,
						pin);

		if (a == null) {
			JOptionPane.showMessageDialog(
					this,
					"开户失败：可能已经存在银行卡账户。");

		} else {
			JOptionPane.showMessageDialog(
					this,
					"开户成功，银行卡号：" + a.getBankCardId());
			refresh();
		}
	}

	private String askBankPIN() {
		JPasswordField[] pinFields = new JPasswordField[6];
		JButton showButton = new JButton("显示");
		JPanel pinPanel =
				createPINPanel(pinFields, showButton);
		JPanel content =
				new JPanel(new FlowLayout(
						FlowLayout.LEFT, 5, 5));

		content.add(new JLabel("银行卡密码:"));
		content.add(pinPanel);
		int result = JOptionPane.showConfirmDialog(
				this,
				content,
				"请输入银行卡密码",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (result != JOptionPane.OK_OPTION) {
			return null;
		}
		String pin = getPIN(pinFields);
		if (!pin.matches("\\d{6}")) {
			JOptionPane.showMessageDialog(
					this,
					"请输入完整的6位银行卡密码。");

			return null;
		}
		return pin;
	}

	private void amountAction(String action) {
		String s = JOptionPane.showInputDialog(
				this,
				action + "金额：");
		if (s == null) {
			return;
		}
		try {
			double amount = Double.parseDouble(s);
			Message m;
			while (true) {
				String pin = askBankPIN();
				if (pin == null) {
					return;
				}
				if ("存钱".equals(action)) {
					m = BankClientSrv.deposit(
							holderCard,
							amount,
							pin);
				} else if ("取钱".equals(action)) {
					m = BankClientSrv.withdraw(
							holderCard,
							amount,
							pin);
				} else {
					m = BankClientSrv.rechargeCampus(
							holderCard,
							amount,
							pin);
				}
				if (m.isSuccess()) {
					break;
				}
				if ("银行卡密码错误，请重新输入。"
						.equals(m.getResponseMsg())) {
					JOptionPane.showMessageDialog(
							this,
							"银行卡密码错误，请重新输入。",
							"密码错误",
							JOptionPane.WARNING_MESSAGE);
					continue;
				}
				break;
			}
			showResult(
					m,
					action + (m.isSuccess() ? "成功" : "失败"));
			if (m.isSuccess()) {
				refresh();
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(
					this,
					"请输入有效数字。");
		}
	}

	private void transfer() {
		JTextField target = new JTextField();
		JTextField amount = new JTextField();

		JPanel p = new JPanel(
				new GridLayout(2, 2, 8, 8));

		p.add(new JLabel("收款银行卡号："));
		p.add(target);
		p.add(new JLabel("转账金额："));
		p.add(amount);

		if (JOptionPane.showConfirmDialog(
				this,
				p,
				"银行卡转账",
				JOptionPane.OK_CANCEL_OPTION)
				!= JOptionPane.OK_OPTION) {
			return;
		}

		try {
			Message m;
			while (true) {
				String pin = askBankPIN();
				if (pin == null) {
					return;
				}
				m = BankClientSrv.transfer(
						holderCard,
						target.getText().trim(),
						Double.parseDouble(
								amount.getText().trim()),
						pin);
				if (m.isSuccess()) {
					break;
				}

				if ("银行卡密码错误，请重新输入。"
						.equals(m.getResponseMsg())) {
					JOptionPane.showMessageDialog(
							this,
							"银行卡密码错误，请重新输入。",
							"密码错误",
							JOptionPane.WARNING_MESSAGE);
					continue;
				}
				break;
			}
			showResult(
					m,
					"转账" + (m.isSuccess() ? "成功" : "失败"));
			if (m.isSuccess()) {
				refresh();
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(
					this,
					"金额格式错误。");
		}
	}

	/**
	 * 后台自动刷新。
	 * 网络请求不在 Swing EDT 中执行，避免 5 秒轮询造成界面卡顿。
	 */
	private void refreshAsync() {
		if (refreshRunning) {
			return;
		}

		refreshRunning = true;

		new SwingWorker<RefreshData, Void>() {
			@Override
			protected RefreshData doInBackground() {
				RefreshData data = new RefreshData();
				data.account = BankClientSrv.getAccount(holderCard);
				data.transactions = BankClientSrv.getTransactions(holderCard);

				if (adminModel != null && currentUser instanceof Admin) {
					data.adminAccounts = BankClientSrv.getAllAccounts(
							((Admin) currentUser).getACard());
				}
				return data;
			}

			@Override
			protected void done() {
				try {
					RefreshData data = get();
					applyRefreshData(data);
				} catch (Exception ignored) {
					// 自动刷新失败时保留当前页面数据，不弹出错误窗口。
				} finally {
					refreshRunning = false;
				}
			}
		}.execute();
	}

	private void applyRefreshData(RefreshData data) {
		BankAccount a = data.account;

		if (a == null) {
			cardLabel.setText("银行卡号：尚未开户");
			balanceLabel.setText("余额：--");
			statusLabel.setText("状态：--");
		} else {
			cardLabel.setText("银行卡号：" + a.getBankCardId());
			balanceLabel.setText(String.format("余额：%.2f 元", a.getBalance()));
			statusLabel.setText("状态：" + (a.getStatus() == 1 ? "正常" : "冻结"));
		}

		if (txModel != null && data.transactions != null) {
			updateTransactionTable(data.transactions);
		}

		if (adminModel != null && data.adminAccounts != null) {
			adminAccounts = data.adminAccounts;
			filterAdminTable();
		}
	}

	private String formatTransactionAmount(BankTransaction t) {
		String type = t.getTransactionType();

		if ("WITHDRAW".equals(type)
				|| "TRANSFER_OUT".equals(type)
				|| "CAMPUS_RECHARGE".equals(type)
				|| "SHOP_PAYMENT".equals(type)
				|| "DORM_PAYMENT".equals(type)) {
			return "-" + String.format("%.2f", t.getAmount());
		}

		return "+" + String.format("%.2f", t.getAmount());
	}

	private void updateTransactionTable(List<BankTransaction> list) {
		txModel.setRowCount(0);
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		for (BankTransaction t : list) {
			String amountText = formatTransactionAmount(t);
			txModel.addRow(new Object[] {
					t.getTransactionTime() == null ? "" : f.format(t.getTransactionTime()),
					t.getTransactionType(),
					amountText,
					t.getTargetId() == null ? "" : t.getTargetId(),
					String.format("%.2f", t.getBalanceAfter()),
					t.getDescription()
			});
		}
	}

	private static class RefreshData {
		private BankAccount account;
		private List<BankTransaction> transactions;
		private List<BankAccount> adminAccounts;
	}

	public void refresh() {
		BankAccount a =
				BankClientSrv.getAccount(holderCard);

		if (a == null) {
			cardLabel.setText("银行卡号：尚未开户");
			balanceLabel.setText("余额：--");
			statusLabel.setText("状态：--");

		} else {
			cardLabel.setText(
					"银行卡号：" + a.getBankCardId());
			balanceLabel.setText(
					String.format(
							"余额：%.2f 元",
							a.getBalance()));
			statusLabel.setText(
					"状态：" +
							(a.getStatus() == 1
									? "正常"
									: "冻结"));
		}

		if (txModel != null) {
			txModel.setRowCount(0);
			List<BankTransaction> list =
					BankClientSrv.getTransactions(holderCard);
			SimpleDateFormat f =
					new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");
			for (BankTransaction t : list) {
				String amountText = formatTransactionAmount(t);
				txModel.addRow(new Object[] {
						t.getTransactionTime() == null
								? ""
								: f.format(
										t.getTransactionTime()),
						t.getTransactionType(),
						amountText,
						t.getTargetId() == null
								? ""
								: t.getTargetId(),
						String.format(
								"%.2f",
								t.getBalanceAfter()),
						t.getDescription()
				});
			}
		}
		if (adminModel != null) {
			refreshAdmin();
		}
	}

	private void filterAdminTable() {
		if (adminAccounts == null) {
			return;
		}
		String keyword =
				adminSearchField.getText()
						.trim()
						.toLowerCase();
		adminModel.setRowCount(0);
		for (BankAccount a : adminAccounts) {
			String status =
					a.getStatus() == 1
							? "正常"
							: "冻结";
			if (keyword.isEmpty()
					|| a.getBankCardId()
							.toLowerCase()
							.contains(keyword)
					|| a.getHolderName()
							.toLowerCase()
							.contains(keyword)
					|| a.getHolderCard()
							.toLowerCase()
							.contains(keyword)
					|| status.toLowerCase()
							.contains(keyword)) {
				adminModel.addRow(new Object[] {
						a.getBankCardId(),
						a.getHolderName(),
						a.getHolderCard(),
						String.format(
								"%.2f",
								a.getBalance()),
						status,
						"查看明细"
				});
			}
		}
	}

	private void refreshAdmin() {
		if (adminModel == null) {
			return;
		}
		adminModel.setRowCount(0);
		adminAccounts =
				BankClientSrv.getAllAccounts(
						((Admin) currentUser).getACard());
		for (BankAccount a : adminAccounts) {
			adminModel.addRow(new Object[] {
					a.getBankCardId(),
					a.getHolderName(),
					a.getHolderCard(),
					String.format(
							"%.2f",
							a.getBalance()),
					a.getStatus() == 1
							? "正常"
							: "冻结",
					"查看明细"
			});
		}
	}

	private class ButtonRenderer extends JButton
			implements javax.swing.table.TableCellRenderer {
		private static final long serialVersionUID = 1L;
		public ButtonRenderer() {
			setText("查看明细");
			setOpaque(true);
			setFont(new Font(
					"微软雅黑",
					Font.PLAIN,
					12));
			setFocusPainted(false);
			setCursor(new Cursor(
					Cursor.HAND_CURSOR));
		}
		@Override
		public Component getTableCellRendererComponent(
				JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column) {
			setText("查看明细");
			return this;
		}
	}

	private void showAdminTransactions(String bankCardId, String holderName, String holderCard) {
	    JDialog dialog = new JDialog(
	            SwingUtilities.getWindowAncestor(this),
	            "银行卡交易明细",
	            Dialog.ModalityType.APPLICATION_MODAL);

	    dialog.setSize(850, 450);
	    dialog.setLocationRelativeTo(this);

	    JPanel panel = new JPanel(new BorderLayout(10, 10));
	    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    panel.setBackground(Color.WHITE);

	    // 获取当前账户最终余额
	    BankAccount account = BankClientSrv.getAccount(holderCard);
	    String finalBalance = account == null
	            ? "—"
	            : String.format("%.2f", account.getBalance());

	    // ===== 顶部账户信息 =====
	    JPanel headerPanel = new JPanel(new GridBagLayout());
	    headerPanel.setBackground(Color.WHITE);

	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(3, 5, 3, 15);
	    gbc.anchor = GridBagConstraints.WEST;
	    gbc.fill = GridBagConstraints.HORIZONTAL;

	    // 第一行：银行卡号 + 持卡人
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.weightx = 0.5;
	    headerPanel.add(infoLabel("银行卡号： " + bankCardId), gbc);

	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.weightx = 0.5;
	    headerPanel.add(infoLabel("持卡人： " + holderName), gbc);

	    // 第二行：一卡通号 + 最终余额
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    headerPanel.add(infoLabel("一卡通号： " + holderCard), gbc);

	    gbc.gridx = 1;
	    gbc.gridy = 1;
	    JLabel balanceLabel = infoLabel("余额： " + finalBalance);
	    headerPanel.add(balanceLabel, gbc);

	    // 第三行：交易明细
	    gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.gridwidth = 2;
	    gbc.weightx = 1.0;
	    JLabel recordTitle = new JLabel("交易明细");
	    recordTitle.setFont(new Font("微软雅黑", Font.BOLD, 18));
	    recordTitle.setForeground(new Color(50, 50, 50));
	    recordTitle.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
	    headerPanel.add(recordTitle, gbc);

	    panel.add(headerPanel, BorderLayout.NORTH);

	    // ===== 交易记录表格 =====
	    DefaultTableModel model = new DefaultTableModel(
	            new String[] { "时间", "类型", "金额", "对方/一卡通", "余额", "说明" }, 0) {

	        private static final long serialVersionUID = 1L;

	        @Override
	        public boolean isCellEditable(int row, int column) {
	            return false;
	        }
	    };

	    JTable table = new JTable(model);
	    configureTransactionTable(table);

	    List<BankTransaction> transactions =
	            BankClientSrv.getTransactions(holderCard);

	    SimpleDateFormat f =
	            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	    for (BankTransaction t : transactions) {

	        String amountText = formatTransactionAmount(t);
	        model.addRow(new Object[] {
	                t.getTransactionTime() == null
	                        ? ""
	                        : f.format(t.getTransactionTime()),

	                t.getTransactionType(),

	                amountText,

	                t.getTargetId() == null
	                        ? ""
	                        : t.getTargetId(),

	                String.format("%.2f", t.getBalanceAfter()),

	                t.getDescription()
	        });
	    }

	    JScrollPane scrollPane = new JScrollPane(table);
	    panel.add(scrollPane, BorderLayout.CENTER);

	    // ===== 关闭按钮 =====
	    JButton closeButton = new JButton("关闭");
	    closeButton.addActionListener(e -> dialog.dispose());

	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    buttonPanel.setBackground(Color.WHITE);
	    buttonPanel.add(closeButton);

	    panel.add(buttonPanel, BorderLayout.SOUTH);

	    dialog.setContentPane(panel);
	    dialog.setVisible(true);
	}

	private void changeAdminStatus(int status) {
		int row =adminTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(
					this,
					"请先选择账户。");
			return;
		}
		String card = String.valueOf(adminModel.getValueAt(row,0));

		Message m =
				BankClientSrv.setStatus(
						((Admin) currentUser).getACard(),
						card,
						status);
		showResult(m,status == 1? "解除冻结" : "冻结");
		if (m.isSuccess()) {
			refreshAdmin();
		}
	}
	
	private void showResult(
			Message m,
			String title) {
		if (!m.isSuccess()) {
			JOptionPane.showMessageDialog(
					this,
					m.getResponseMsg() == null
							? title + "失败"
							: m.getResponseMsg());
		}
	}
	
	private static String cardOf(Object u) {
		if (u instanceof Student) {
			return ((Student) u).getSCard();
		}
		if (u instanceof Teacher) {
			return ((Teacher) u).getTeacherCard();
		}
		if (u instanceof Admin) {
			return ((Admin) u).getACard();
		}
		return "";
	}
	private static String nameOf(Object u) {
		if (u instanceof Student) {
			return ((Student) u).getSname();
		}
		if (u instanceof Teacher) {
			return ((Teacher) u).getTeacherName();
		}
		if (u instanceof Admin) {
			return ((Admin) u).getAName();
		}
		return "用户";
	}
}