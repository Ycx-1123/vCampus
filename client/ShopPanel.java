/**
 * 商店客户端主界面（Swing）
 * 支持普通用户购物和管理员库存管理
 * 商品以卡片网格展示，带图片
 * 
 * ToDo
 * 1、刷新功能是否和库存有关 按理说直接自动显示
 * 2、管理员增加商品没有图片
 * 3、useraccount的作用是否冗余
 * 4、当商品为0时管理员能否被提醒
 */
package vCampus.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import vCampus.common.Message;
import vCampus.common.shop.Order;
import vCampus.common.shop.OrderItem;
import vCampus.common.shop.OrderStatus;
import vCampus.common.shop.Product;
import vCampus.common.shop.UserAccount;
import vCampus.common.vo.Admin;
import vCampus.common.vo.Student;
import vCampus.common.vo.Teacher;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import java.io.File;
import java.net.URL;

public class ShopPanel extends JPanel {

	// ---------- 数据 ----------
	private List<Product> allProducts = new ArrayList<>();
	private List<Product> currentProducts = new ArrayList<>();
	private List<OrderItem> cartItems = new ArrayList<>();
	private Map<Long, List<OrderItem>> orderItemMap = new HashMap<>();
	private Set<Integer> favoriteProductIds = new HashSet<>();
	private double redPacketAmount = 0;
	private boolean showingFavoritesOnly = false;

	private Object loginUser;
	private boolean isShopManager;
	private boolean managerMode;
	private boolean stockRefreshInProgress = false;
	private javax.swing.Timer stockRefreshTimer;
	private Image backgroundImage;

	// UI 组件
	private JList<Product> productList;
	private DefaultListModel<Product> productListModel;
	private JComboBox<String> categoryCombo;
	private JButton btnViewCart;
	private JButton btnViewOrders;
	private JLabel userLabel;

	private JDialog cartDialog;
	private JTable cartTable;
	private DefaultTableModel cartTableModel;
	private JLabel cartTotalLabel;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	// 颜色方案
	private static final Color COLOR_PRIMARY = new Color(52, 152, 219);
	private static final Color COLOR_SECONDARY = new Color(46, 204, 113);
	private static final Color COLOR_ORANGE = new Color(243, 156, 18);
	private static final Color COLOR_BG = new Color(245, 247, 250);
	private static final Color COLOR_TABLE_ODD = new Color(240, 244, 248);
	private static final Color COLOR_DARK_BTN = new Color(52, 73, 94);

	// ---------- 构造方法 ----------
	public ShopPanel(Object user) {
		this.loginUser = user;
		this.backgroundImage = loadShopBackgroundImage();
		if (user instanceof Admin) {
			Admin admin = (Admin) user;
			isShopManager = admin.isManageShop() || "李商店".equals(admin.getAName());
			managerMode = false;
		} else {
			isShopManager = false;
			managerMode = false;
		}

		loadProductsFromServer();
		initUI();
		filterProducts("全部");
		refreshProductList();
		updateUserLabel();
		startStockAutoRefresh();
	}

	public ShopPanel() {
		this(null);
	}

	@Override
	public void removeNotify() {
		stopStockAutoRefresh();
		super.removeNotify();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (backgroundImage != null) {
			Graphics2D g2 = (Graphics2D) g.create();
			int panelWidth = getWidth();
			int panelHeight = getHeight();
			int imageWidth = backgroundImage.getWidth(this);
			int imageHeight = backgroundImage.getHeight(this);
			if (imageWidth > 0 && imageHeight > 0) {
				double scale = Math.max(panelWidth * 1.0 / imageWidth, panelHeight * 1.0 / imageHeight);
				int drawWidth = (int) (imageWidth * scale);
				int drawHeight = (int) (imageHeight * scale);
				int x = (panelWidth - drawWidth) / 2;
				int y = (panelHeight - drawHeight) / 2;
				g2.drawImage(backgroundImage, x, y, drawWidth, drawHeight, this);
				g2.setColor(new Color(255, 255, 255, 175));
				g2.fillRect(0, 0, panelWidth, panelHeight);
			}
			g2.dispose();
		}
	}

	private Image loadShopBackgroundImage() {
		String[] names = { "seu_auditorium", "auditorium", "dalitang" };
		String[] extensions = { ".png", ".jpg", ".jpeg" };
		String[] dirs = { "images", "../images", "vCampusTest/images", "src/images" };

		for (String dir : dirs) {
			for (String name : names) {
				for (String ext : extensions) {
					File file = new File(dir, name + ext);
					if (file.exists()) {
						return new ImageIcon(file.getAbsolutePath()).getImage();
					}
				}
			}
		}

		for (String name : names) {
			for (String ext : extensions) {
				URL url = getClass().getResource("/images/" + name + ext);
				if (url != null) {
					return new ImageIcon(url).getImage();
				}
			}
		}
		return null;
	}

	private void startStockAutoRefresh() {
		stockRefreshTimer = new javax.swing.Timer(5000, e -> refreshProductsSilently());
		stockRefreshTimer.setInitialDelay(5000);
		stockRefreshTimer.start();
	}

	private void stopStockAutoRefresh() {
		if (stockRefreshTimer != null) {
			stockRefreshTimer.stop();
			stockRefreshTimer = null;
		}
	}

	private void refreshProductsSilently() {
		if (stockRefreshInProgress)
			return;
		stockRefreshInProgress = true;

		new Thread(() -> {
			List<Product> products = null;
			try {
				products = ShopClientSrv.getAllProducts();
			} catch (Exception e) {
				e.printStackTrace();
			}

			final List<Product> fetchedProducts = products;
			SwingUtilities.invokeLater(() -> {
				try {
					if (fetchedProducts != null && !fetchedProducts.isEmpty()) {
						allProducts = fetchedProducts;
						filterProducts((String) categoryCombo.getSelectedItem());
						syncCartWithCurrentStock();
						refreshCartDialog();
					}
				} finally {
					stockRefreshInProgress = false;
				}
			});
		}).start();
	}

	// ---------- 加载商品 ----------
	private void loadProductsFromServer() {
		try {
			List<Product> products = ShopClientSrv.getAllProducts();
			if (products != null && !products.isEmpty()) {
				allProducts = products;
				System.out.println("✅ 从服务器加载商品 " + products.size() + " 件");
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 降级：虚拟数据
		System.out.println("⚠️ 使用虚拟商品数据");
		allProducts.clear();
		allProducts.add(new Product(1, "洗发水", 35.0, 20, "去屑止痒", "日用品"));
		allProducts.add(new Product(2, "毛巾", 15.0, 50, "纯棉吸水", "日用品"));
		allProducts.add(new Product(3, "T恤", 89.0, 30, "纯棉白色", "服装"));
		allProducts.add(new Product(4, "牛仔裤", 199.0, 15, "修身款", "服装"));
		allProducts.add(new Product(5, "苹果", 6.5, 100, "红富士", "食品"));
		allProducts.add(new Product(6, "牛奶", 3.0, 60, "纯牛奶", "食品"));
		allProducts.add(new Product(7, "U盘 64G", 45.0, 20, "USB3.0", "电子"));
		allProducts.add(new Product(8, "蓝牙耳机", 159.0, 8, "降噪", "电子"));
	}

	// ---------- 提取用户信息 ----------
	private String extractUserId(Object user) {
		if (user instanceof Student)
			return ((Student) user).getStuCard();
		if (user instanceof Admin) {
			Admin admin = (Admin) user;
			return admin.getACard() != null ? admin.getACard() : admin.getAId();
		}
		if (user instanceof Teacher)
			return ((Teacher) user).getTeacherCard();
		if (user instanceof UserAccount)
			return ((UserAccount) user).getUserId();
		return "未登录";
	}

	private String extractUserName(Object user) {
		if (user instanceof Student)
			return ((Student) user).getSname();
		if (user instanceof Admin)
			return ((Admin) user).getAName();
		if (user instanceof Teacher)
			return ((Teacher) user).getTeacherName();
		if (user instanceof UserAccount)
			return ((UserAccount) user).getUserId();
		return "游客";
	}

	private String extractRole(Object user) {
		if (user instanceof Student)
			return "学生";
		if (user instanceof Admin)
			return "管理员";
		if (user instanceof Teacher)
			return "教师";
		return "未知";
	}

	// ---------- UI 初始化 ----------
	private void initUI() {
		btnViewCart = null;
		btnViewOrders = null;
		setLayout(new BorderLayout(10, 10));
		setOpaque(false);

		// ===== 顶部标题栏 =====
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(COLOR_PRIMARY);
		titlePanel.setBorder(new EmptyBorder(10, 20, 10, 20));

		JLabel titleLabel = new JLabel("校园商店", JLabel.LEFT);
		titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 28));
		titleLabel.setForeground(Color.WHITE);
		titlePanel.add(titleLabel, BorderLayout.WEST);

		userLabel = new JLabel("加载中...");
		userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		userLabel.setForeground(Color.WHITE);
		titlePanel.add(userLabel, BorderLayout.EAST);

		add(titlePanel, BorderLayout.NORTH);

		// ===== 中心区域 =====
		JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
		centerPanel.setOpaque(false);

		// 分类筛选栏
		JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		filterPanel.setBackground(new Color(255, 255, 255, 210));
		filterPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
		JLabel filterLabel = new JLabel("分类：");
		filterLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
		filterPanel.add(filterLabel);

		categoryCombo = new JComboBox<>(new String[] { "全部", "日用品", "服装", "食品", "电子" });
		categoryCombo.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		categoryCombo.addActionListener(e -> filterProducts((String) categoryCombo.getSelectedItem()));
		filterPanel.add(categoryCombo);

		if (!managerMode) {
			JButton btnFavorite = new JButton("收藏/取消收藏");
			JButton btnFavoritesOnly = new JButton("我的收藏");
			JButton btnSubsidy = new JButton("国补抽红包");
			styleSmallButton(btnFavorite, COLOR_SECONDARY);
			styleSmallButton(btnFavoritesOnly, COLOR_ORANGE);
			styleSmallButton(btnSubsidy, new Color(231, 76, 60));
			btnFavorite.addActionListener(e -> toggleFavoriteSelectedProduct());
			btnFavoritesOnly.addActionListener(e -> {
				showingFavoritesOnly = !showingFavoritesOnly;
				btnFavoritesOnly.setText(showingFavoritesOnly ? "全部商品" : "我的收藏");
				filterProducts((String) categoryCombo.getSelectedItem());
			});
			btnSubsidy.addActionListener(e -> showSubsidyLottery());
			filterPanel.add(btnFavorite);
			filterPanel.add(btnFavoritesOnly);
			filterPanel.add(btnSubsidy);
		}
		centerPanel.add(filterPanel, BorderLayout.NORTH);

		// 商品列表（卡片网格）
		productListModel = new DefaultListModel<>();
		productList = new JList<>(productListModel);
		productList.setCellRenderer(new ProductListRenderer());
		productList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		productList.setVisibleRowCount(-1);
		productList.setFixedCellWidth(190);
		productList.setFixedCellHeight(240);
		productList.setOpaque(false);
		productList.setBackground(new Color(0, 0, 0, 0));

		// 双击加入购物车
		productList.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (!managerMode && e.getClickCount() == 2) {
					Product p = productList.getSelectedValue();
					if (p != null)
						addToCartFromProduct(p);
				}
			}
		});

		JScrollPane productScroll = new JScrollPane(productList);
		productScroll.setOpaque(false);
		productScroll.getViewport().setOpaque(false);
		productScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_PRIMARY, 2),
				managerMode ? "库存列表（选择商品后进行管理）" : "商品列表（双击卡片加入购物车）", TitledBorder.LEFT, TitledBorder.TOP,
				new Font("微软雅黑", Font.BOLD, 14), COLOR_PRIMARY));
		centerPanel.add(productScroll, BorderLayout.CENTER);
		add(centerPanel, BorderLayout.CENTER);

		// ===== 底部按钮 =====
		JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
		bottomPanel.setBackground(new Color(255, 255, 255, 210));
		bottomPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

		JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 5));
		mainButtonPanel.setOpaque(false);

		if (managerMode) {
			JButton btnAdd = new JButton("上架商品");
			JButton btnStock = new JButton("增加库存");
			JButton btnDelete = new JButton("下架商品");
			JButton btnRefresh = new JButton("刷新");
			styleBottomButton(btnAdd, COLOR_SECONDARY);
			styleBottomButton(btnStock, COLOR_PRIMARY);
			styleBottomButton(btnDelete, new Color(231, 76, 60));
			styleBottomButton(btnRefresh, COLOR_DARK_BTN);
			btnAdd.addActionListener(e -> addProductByDialog(this, null));
			btnStock.addActionListener(e -> addStockSelectedProduct(this));
			btnDelete.addActionListener(e -> deleteSelectedProduct(this));
			btnRefresh.addActionListener(e -> reloadProducts(null));
			mainButtonPanel.add(btnAdd);
			mainButtonPanel.add(btnStock);
			mainButtonPanel.add(btnDelete);
			mainButtonPanel.add(btnRefresh);
		} else {
			btnViewCart = new JButton("我的购物车 (0)");
			styleBottomButton(btnViewCart, COLOR_SECONDARY);
			btnViewCart.addActionListener(e -> showCartDialog());
			mainButtonPanel.add(btnViewCart);

			btnViewOrders = new JButton("我的历史订单");
			styleBottomButton(btnViewOrders, COLOR_ORANGE);
			btnViewOrders.addActionListener(e -> showOrderHistory());
			mainButtonPanel.add(btnViewOrders);
		}

		bottomPanel.add(mainButtonPanel, BorderLayout.CENTER);
		if (isShopManager) {
			JButton btnSwitchMode = createRoundSwitchButton();
			btnSwitchMode.addActionListener(e -> switchShopMode());
			JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			switchPanel.setOpaque(false);
			switchPanel.add(btnSwitchMode);
			bottomPanel.add(switchPanel, BorderLayout.EAST);
		}

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void switchShopMode() {
		managerMode = !managerMode;
		showingFavoritesOnly = false;
		if (cartDialog != null) {
			cartDialog.setVisible(false);
		}

		removeAll();
		initUI();
		filterProducts((String) categoryCombo.getSelectedItem());
		refreshProductList();
		updateUserLabel();
		revalidate();
		repaint();
	}

	// ---------- 内部类：商品卡片渲染器 ----------
	private class ProductListRenderer extends JPanel implements ListCellRenderer<Product> {
		private JLabel lblImage;
		private JLabel lblName;
		private JLabel lblPrice;
		private JLabel lblStock;

		public ProductListRenderer() {
			setLayout(new BorderLayout(5, 5));
			setBackground(Color.WHITE);
			setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true));
			setPreferredSize(new Dimension(180, 220));

			lblImage = new JLabel();
			lblImage.setHorizontalAlignment(SwingConstants.CENTER);
			lblImage.setPreferredSize(new Dimension(160, 120));
			add(lblImage, BorderLayout.CENTER);

			JPanel infoPanel = new JPanel(new GridLayout(3, 1, 2, 2));
			infoPanel.setBackground(Color.WHITE);
			infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

			lblName = new JLabel();
			lblName.setFont(new Font("微软雅黑", Font.BOLD, 13));
			lblPrice = new JLabel();
			lblPrice.setFont(new Font("微软雅黑", Font.BOLD, 14));
			lblPrice.setForeground(new Color(220, 50, 50));
			lblStock = new JLabel();
			lblStock.setFont(new Font("微软雅黑", Font.PLAIN, 11));
			lblStock.setForeground(new Color(150, 150, 150));

			infoPanel.add(lblName);
			infoPanel.add(lblPrice);
			infoPanel.add(lblStock);
			add(infoPanel, BorderLayout.SOUTH);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Product> list, Product p, int index,
				boolean isSelected, boolean cellHasFocus) {
			// 加载图片
			ImageIcon icon = loadProductImage(p);
			if (icon != null) {
				Image scaled = icon.getImage().getScaledInstance(160, 120, Image.SCALE_SMOOTH);
				lblImage.setIcon(new ImageIcon(scaled));
				lblImage.setText(null);
			} else {
				lblImage.setIcon(null);
				lblImage.setText("📦");
				lblImage.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
			}
			lblName.setText((favoriteProductIds.contains(p.getId()) ? "★ " : "") + p.getName());
			lblPrice.setText("¥ " + String.format("%.2f", p.getPrice()));
			lblStock.setText("库存: " + p.getStock());

			setBorder(isSelected ? BorderFactory.createLineBorder(new Color(52, 152, 219), 2, true)
					: BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true));
			setBackground(isSelected ? new Color(240, 248, 255) : Color.WHITE);
			return this;
		}

		private ImageIcon loadProductImage(Product p) {
			String baseName = "product_" + p.getId();
			String[] extensions = { ".png", ".jpg", ".jpeg" };
			String[] dirs = { "images", "../images", "vCampusTest/images", "src/images" };

			for (String dir : dirs) {
				for (String ext : extensions) {
					File file = new File(dir, baseName + ext);
					if (file.exists()) {
						return new ImageIcon(file.getAbsolutePath());
					}
				}
			}

			for (String ext : extensions) {
				URL url = getClass().getResource("/images/" + baseName + ext);
				if (url != null) {
					return new ImageIcon(url);
				}
			}
			return null;
		}
	}

	// ---------- 更新界面 ----------
	private void updateUserLabel() {
		if (loginUser != null) {
			String id = extractUserId(loginUser);
			String name = extractUserName(loginUser);
			String role = extractRole(loginUser);
			userLabel.setText(name + " (" + id + ")  |  " + role);
		} else {
			userLabel.setText("未登录 (测试模式)");
		}
	}

	private void filterProducts(String category) {
		if ("全部".equals(category)) {
			currentProducts = new ArrayList<>(allProducts);
		} else {
			currentProducts = allProducts.stream().filter(p -> category.equals(p.getCategory()))
					.collect(Collectors.toList());
		}
		if (showingFavoritesOnly) {
			currentProducts = currentProducts.stream().filter(p -> favoriteProductIds.contains(p.getId()))
					.collect(Collectors.toList());
		}
		refreshProductList();
	}

	private void refreshProductList() {
		if (productListModel == null)
			return;
		productListModel.clear();
		for (Product p : currentProducts) {
			productListModel.addElement(p);
		}
		updateCartButtonText();
	}

	private void updateCartButtonText() {
		if (btnViewCart != null) {
			btnViewCart.setText("我的购物车 (" + cartItems.size() + ")");
		}
	}

	private void styleSmallButton(JButton button, Color color) {
		button.setFont(new Font("微软雅黑", Font.BOLD, 13));
		button.setBackground(color);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		button.setPreferredSize(new Dimension(120, 32));
	}

	private void styleBottomButton(JButton button, Color color) {
		button.setFont(new Font("微软雅黑", Font.BOLD, 16));
		button.setBackground(color);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		button.setPreferredSize(new Dimension(200, 45));
	}

	private JButton createRoundSwitchButton() {
		JButton button = new JButton(managerMode ? "购" : "管") {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(getModel().isPressed() ? COLOR_PRIMARY : COLOR_DARK_BTN);
				g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
				g2.dispose();
				super.paintComponent(g);
			}

			@Override
			protected void paintBorder(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(255, 255, 255, 180));
				g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
				g2.dispose();
			}

			@Override
			public boolean contains(int x, int y) {
				int radius = Math.min(getWidth(), getHeight());
				int centerX = getWidth() / 2;
				int centerY = getHeight() / 2;
				int dx = x - centerX;
				int dy = y - centerY;
				return dx * dx + dy * dy <= radius * radius / 4;
			}
		};
		button.setFont(new Font("微软雅黑", Font.BOLD, 18));
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setPreferredSize(new Dimension(46, 46));
		button.setToolTipText(managerMode ? "切换到购物" : "切换到管理");
		return button;
	}

	private JButton createQuantityButton(String text) {
		JButton button = new JButton(text);
		button.setFont(new Font("微软雅黑", Font.BOLD, 22));
		button.setForeground(Color.WHITE);
		button.setBackground(COLOR_PRIMARY);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		Dimension size = new Dimension(42, 36);
		button.setPreferredSize(size);
		button.setMinimumSize(size);
		button.setMaximumSize(size);
		return button;
	}

	private Integer showQuantityDialog(Product product) {
		final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "选择购买数量",
				Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setSize(360, 210);
		dialog.setLocationRelativeTo(this);
		dialog.setLayout(new BorderLayout(8, 8));
		dialog.getContentPane().setBackground(Color.WHITE);

		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(COLOR_PRIMARY);
		titlePanel.setBorder(new EmptyBorder(9, 16, 9, 16));
		JLabel title = new JLabel("选择购买数量");
		title.setFont(new Font("微软雅黑", Font.BOLD, 17));
		title.setForeground(Color.WHITE);
		titlePanel.add(title, BorderLayout.WEST);
		dialog.add(titlePanel, BorderLayout.NORTH);

		JPanel content = new JPanel(new BorderLayout(6, 8));
		content.setBackground(Color.WHITE);
		content.setBorder(new EmptyBorder(14, 20, 6, 20));

		JPanel quantityPanel = new JPanel(new GridBagLayout());
		quantityPanel.setBackground(Color.WHITE);
		JButton btnMinus = createQuantityButton("-");
		JButton btnPlus = createQuantityButton("+");
		JTextField txtQty = new JTextField("1");
		txtQty.setHorizontalAlignment(JTextField.CENTER);
		txtQty.setFont(new Font("微软雅黑", Font.BOLD, 18));
		Dimension inputSize = new Dimension(94, 36);
		txtQty.setPreferredSize(inputSize);
		txtQty.setMinimumSize(inputSize);
		txtQty.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(180, 205, 230), 1),
				new EmptyBorder(3, 8, 3, 8)));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 6, 0, 6);
		quantityPanel.add(btnMinus, gbc);
		quantityPanel.add(txtQty, gbc);
		quantityPanel.add(btnPlus, gbc);
		content.add(quantityPanel, BorderLayout.CENTER);

		JLabel hint = new JLabel("可点击加减号，或直接输入 1 - " + product.getStock());
		hint.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		hint.setForeground(new Color(120, 120, 120));
		hint.setHorizontalAlignment(SwingConstants.CENTER);
		content.add(hint, BorderLayout.SOUTH);
		dialog.add(content, BorderLayout.CENTER);

		final Integer[] result = new Integer[1];
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
		buttonPanel.setBackground(new Color(245, 247, 250));
		JButton btnOk = new JButton("加入购物车");
		JButton btnCancel = new JButton("取消");
		styleSmallButton(btnOk, COLOR_SECONDARY);
		styleSmallButton(btnCancel, COLOR_DARK_BTN);
		btnOk.setPreferredSize(new Dimension(118, 32));
		btnCancel.setPreferredSize(new Dimension(76, 32));
		buttonPanel.add(btnCancel);
		buttonPanel.add(btnOk);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		Runnable normalizeQuantity = () -> {
			try {
				int value = Integer.parseInt(txtQty.getText().trim());
				if (value < 1) value = 1;
				if (value > product.getStock()) value = product.getStock();
				txtQty.setText(String.valueOf(value));
			} catch (NumberFormatException ex) {
				txtQty.setText("1");
			}
		};

		btnMinus.addActionListener(e -> {
			normalizeQuantity.run();
			int value = Integer.parseInt(txtQty.getText().trim());
			if (value > 1) txtQty.setText(String.valueOf(value - 1));
		});
		btnPlus.addActionListener(e -> {
			normalizeQuantity.run();
			int value = Integer.parseInt(txtQty.getText().trim());
			if (value < product.getStock()) txtQty.setText(String.valueOf(value + 1));
		});
		btnCancel.addActionListener(e -> dialog.dispose());
		btnOk.addActionListener(e -> {
			normalizeQuantity.run();
			result[0] = Integer.parseInt(txtQty.getText().trim());
			dialog.dispose();
		});
		txtQty.addActionListener(e -> btnOk.doClick());

		SwingUtilities.invokeLater(() -> {
			txtQty.requestFocusInWindow();
			txtQty.selectAll();
		});
		dialog.setVisible(true);
		return result[0];
	}

	private void toggleFavoriteSelectedProduct() {
		Product product = productList.getSelectedValue();
		if (product == null) {
			JOptionPane.showMessageDialog(this, "请先选择要收藏的商品。", "收藏", JOptionPane.WARNING_MESSAGE);
			return;
		}

		if (favoriteProductIds.contains(product.getId())) {
			favoriteProductIds.remove(product.getId());
			JOptionPane.showMessageDialog(this, "已取消收藏：" + product.getName(), "收藏", JOptionPane.INFORMATION_MESSAGE);
		} else {
			favoriteProductIds.add(product.getId());
			JOptionPane.showMessageDialog(this, "已收藏：" + product.getName(), "收藏", JOptionPane.INFORMATION_MESSAGE);
		}
		filterProducts((String) categoryCombo.getSelectedItem());
	}

	private void showSubsidyLottery() {
		if (redPacketAmount > 0) {
			JOptionPane.showMessageDialog(this, "你已经领取过红包啦。\n当前可用红包：" + String.format("%.2f", redPacketAmount) + " 元",
					"国补红包", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		double[] awards = { 2, 3, 5, 8, 10, 15 };
		redPacketAmount = awards[new Random().nextInt(awards.length)];
		JOptionPane.showMessageDialog(this, "恭喜领取国补红包：" + String.format("%.2f", redPacketAmount) + " 元\n下单结算时会自动抵扣。",
				"国补红包", JOptionPane.INFORMATION_MESSAGE);
		refreshCartDialog();
	}

	private void applyPurchasedStockToLocalProducts(List<OrderItem> purchasedItems) {
		if (purchasedItems == null)
			return;
		for (OrderItem item : purchasedItems) {
			Product product = findProductById(item.getProductId());
			if (product != null) {
				product.setStock(Math.max(0, product.getStock() - item.getQuantity()));
			}
		}
		filterProducts((String) categoryCombo.getSelectedItem());
	}

	private void syncCartWithCurrentStock() {
		for (int i = cartItems.size() - 1; i >= 0; i--) {
			OrderItem item = cartItems.get(i);
			Product product = findProductById(item.getProductId());
			if (product == null || product.getStock() <= 0) {
				cartItems.remove(i);
			} else if (item.getQuantity() > product.getStock()) {
				item.setQuantity(product.getStock());
			}
		}
	}

	// ---------- 购物车功能 ----------
	private void addToCartFromProduct(Product p) {
		if (p == null || p.getStock() <= 0) {
			JOptionPane.showMessageDialog(this, "商品库存不足！", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		Integer selectedQty = showQuantityDialog(p);
		if (selectedQty == null)
			return;
		try {
			int qty = selectedQty;
			if (qty <= 0 || qty > p.getStock()) {
				JOptionPane.showMessageDialog(this, "数量无效！", "错误", JOptionPane.WARNING_MESSAGE);
				return;
			}
			// 检查是否已存在
			for (OrderItem item : cartItems) {
				if (item.getProductId() == p.getId()) {
					int newQty = item.getQuantity() + qty;
					if (newQty > p.getStock()) {
						JOptionPane.showMessageDialog(this, "总数量超过库存！", "错误", JOptionPane.WARNING_MESSAGE);
						return;
					}
					item.setQuantity(newQty);
					refreshCartDialog();
					JOptionPane.showMessageDialog(this, "购物车已更新！", "提示", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
			}
			OrderItem newItem = new OrderItem();
			newItem.setProductId(p.getId());
			newItem.setQuantity(qty);
			newItem.setUnitPrice(p.getPrice());
			cartItems.add(newItem);
			refreshCartDialog();
			JOptionPane.showMessageDialog(this, "已加入购物车！", "提示", JOptionPane.INFORMATION_MESSAGE);
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "请输入有效数字！", "错误", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void showCartDialog() {
		if (cartDialog == null || !cartDialog.isVisible()) {
			createCartDialog();
		}
		refreshCartDialog();
		cartDialog.setVisible(true);
	}

	private void createCartDialog() {
		cartDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "我的购物车");
		cartDialog.setSize(750, 480);
		cartDialog.setLocationRelativeTo(this);
		cartDialog.getContentPane().setBackground(COLOR_BG);
		cartDialog.setLayout(new BorderLayout(10, 10));

		String[] cartCols = { "选择", "商品名称", "分类", "单价(元)", "数量", "小计(元)" };
		cartTableModel = new DefaultTableModel(cartCols, 0) {
			@Override
			public boolean isCellEditable(int row, int col) {
				return col == 0;
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnIndex == 0 ? Boolean.class : Object.class;
			}
		};
		cartTable = new JTable(cartTableModel);
		cartTable.setRowHeight(28);
		cartTable.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		cartTable.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 14));
		cartTable.getTableHeader().setBackground(COLOR_PRIMARY);
		cartTable.getTableHeader().setForeground(Color.WHITE);
		cartTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		cartTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (!isSelected) {
					c.setBackground(row % 2 == 0 ? COLOR_TABLE_ODD : Color.WHITE);
					c.setForeground(Color.BLACK);
				} else {
					c.setBackground(COLOR_PRIMARY);
					c.setForeground(Color.WHITE);
				}
				return c;
			}
		});

		JScrollPane scroll = new JScrollPane(cartTable);
		scroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_PRIMARY, 1), "购物车商品",
				TitledBorder.LEFT, TitledBorder.TOP, new Font("微软雅黑", Font.BOLD, 13), COLOR_PRIMARY));
		cartDialog.add(scroll, BorderLayout.CENTER);

		JPanel bottom = new JPanel(new BorderLayout(10, 10));
		bottom.setBackground(COLOR_BG);
		bottom.setBorder(new EmptyBorder(10, 10, 10, 10));

		cartTotalLabel = new JLabel("总价：0.00 元");
		cartTotalLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
		cartTotalLabel.setForeground(COLOR_ORANGE);
		bottom.add(cartTotalLabel, BorderLayout.WEST);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
		btnPanel.setBackground(COLOR_BG);

		JButton btnSelectAll = new JButton("全选/取消全选");
		btnSelectAll.setFont(new Font("微软雅黑", Font.BOLD, 13));
		btnSelectAll.setBackground(COLOR_DARK_BTN);
		btnSelectAll.setForeground(Color.WHITE);
		btnSelectAll.setFocusPainted(false);
		btnSelectAll.setBorderPainted(false);
		btnSelectAll.setOpaque(true);

		JButton btnRemove = new JButton("移除选中");
		btnRemove.setFont(new Font("微软雅黑", Font.BOLD, 13));
		btnRemove.setBackground(new Color(231, 76, 60));
		btnRemove.setForeground(Color.WHITE);
		btnRemove.setFocusPainted(false);
		btnRemove.setBorderPainted(false);
		btnRemove.setOpaque(true);

		JButton btnCheckout = new JButton("结算选中商品");
		btnCheckout.setFont(new Font("微软雅黑", Font.BOLD, 13));
		btnCheckout.setBackground(COLOR_SECONDARY);
		btnCheckout.setForeground(Color.WHITE);
		btnCheckout.setFocusPainted(false);
		btnCheckout.setBorderPainted(false);
		btnCheckout.setOpaque(true);

		btnPanel.add(btnSelectAll);
		btnPanel.add(btnRemove);
		btnPanel.add(btnCheckout);
		bottom.add(btnPanel, BorderLayout.EAST);

		cartDialog.add(bottom, BorderLayout.SOUTH);

		btnSelectAll.addActionListener(e -> toggleSelectAllCartItems());
		btnRemove.addActionListener(e -> removeSelectedFromCart());
		btnCheckout.addActionListener(e -> checkoutSelected());

		cartTable.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getClickCount() == 2) {
					modifyCartItemQuantity();
				}
			}
		});
	}

	private void refreshCartDialog() {
		if (cartTableModel == null || cartTotalLabel == null) {
			updateCartButtonText();
			return;
		}
		cartTableModel.setRowCount(0);
		double total = 0;
		for (OrderItem item : cartItems) {
			Product p = findProductById(item.getProductId());
			if (p == null)
				continue;
			double subtotal = item.getQuantity() * item.getUnitPrice();
			total += subtotal;
			cartTableModel.addRow(new Object[] { Boolean.FALSE, p.getName(), p.getCategory(), item.getUnitPrice(),
					item.getQuantity(), subtotal });
		}
		double discount = Math.min(redPacketAmount, total);
		double payable = Math.max(0, total - discount);
		if (discount > 0) {
			cartTotalLabel.setText("总价：" + String.format("%.2f", total) + " 元，红包抵扣：" + String.format("%.2f", discount)
					+ " 元，应付：" + String.format("%.2f", payable) + " 元");
		} else {
			cartTotalLabel.setText("总价：" + String.format("%.2f", total) + " 元");
		}
		updateCartButtonText();
	}

	private void removeSelectedFromCart() {
		int[] rows = getCheckedOrSelectedCartRows();
		if (rows.length == 0) {
			JOptionPane.showMessageDialog(cartDialog, "请选择要移除的商品！", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		List<Integer> toRemove = new ArrayList<>();
		for (int r : rows) {
			if (r >= 0 && r < cartItems.size()) {
				toRemove.add(r);
			}
		}
		toRemove.sort((a, b) -> b - a);
		for (int idx : toRemove) {
			cartItems.remove(idx);
		}
		refreshCartDialog();
		JOptionPane.showMessageDialog(cartDialog, "已移除选中商品！", "提示", JOptionPane.INFORMATION_MESSAGE);
	}

	private void modifyCartItemQuantity() {
		int row = cartTable.getSelectedRow();
		if (row == -1 || row >= cartItems.size())
			return;
		OrderItem item = cartItems.get(row);
		Product p = findProductById(item.getProductId());
		if (p == null)
			return;

		String qStr = JOptionPane.showInputDialog(cartDialog,
				"当前数量：" + item.getQuantity() + "（库存：" + p.getStock() + "）\n输入新数量：", "修改数量",
				JOptionPane.QUESTION_MESSAGE);
		if (qStr == null)
			return;
		int qty;
		try {
			qty = Integer.parseInt(qStr);
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(cartDialog, "请输入有效数字！", "错误", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (qty <= 0) {
			cartItems.remove(row);
		} else if (qty > p.getStock()) {
			JOptionPane.showMessageDialog(cartDialog, "数量超过库存！", "错误", JOptionPane.WARNING_MESSAGE);
			return;
		} else {
			item.setQuantity(qty);
		}
		refreshCartDialog();
	}

	private void toggleSelectAllCartItems() {
		if (cartTableModel == null || cartTableModel.getRowCount() == 0)
			return;
		boolean hasUnchecked = false;
		for (int i = 0; i < cartTableModel.getRowCount(); i++) {
			Object value = cartTableModel.getValueAt(i, 0);
			if (!(value instanceof Boolean) || !((Boolean) value)) {
				hasUnchecked = true;
				break;
			}
		}
		for (int i = 0; i < cartTableModel.getRowCount(); i++) {
			cartTableModel.setValueAt(hasUnchecked, i, 0);
		}
	}

	private int[] getCheckedOrSelectedCartRows() {
		List<Integer> rows = new ArrayList<>();
		if (cartTableModel != null) {
			for (int i = 0; i < cartTableModel.getRowCount(); i++) {
				Object value = cartTableModel.getValueAt(i, 0);
				if (value instanceof Boolean && ((Boolean) value)) {
					rows.add(i);
				}
			}
		}
		if (rows.isEmpty() && cartTable != null) {
			for (int row : cartTable.getSelectedRows()) {
				rows.add(row);
			}
		}
		int[] result = new int[rows.size()];
		for (int i = 0; i < rows.size(); i++) {
			result[i] = rows.get(i);
		}
		return result;
	}

	// ---------- 下单（包含支付） ----------
	private void checkoutSelected() {

	    if (cartItems.isEmpty()) {
	        JOptionPane.showMessageDialog(
	                this,
	                "购物车为空！",
	                "提示",
	                JOptionPane.WARNING_MESSAGE);
	        return;
	    }

	    int[] selectedRows = getCheckedOrSelectedCartRows();

	    if (selectedRows.length == 0) {
	        JOptionPane.showMessageDialog(
	                cartDialog,
	                "请选择要结算的商品！",
	                "提示",
	                JOptionPane.WARNING_MESSAGE);
	        return;
	    }

	    List<OrderItem> selectedItems = new ArrayList<>();
	    List<Integer> selectedIndexes = new ArrayList<>();

	    for (int row : selectedRows) {

	        if (row >= 0 && row < cartItems.size()) {

	            OrderItem source = cartItems.get(row);

	            OrderItem copy = new OrderItem();
	            copy.setProductId(source.getProductId());
	            copy.setQuantity(source.getQuantity());
	            copy.setUnitPrice(source.getUnitPrice());

	            selectedItems.add(copy);
	            selectedIndexes.add(row);
	        }
	    }

	    if (selectedItems.isEmpty()) {
	        JOptionPane.showMessageDialog(
	                cartDialog,
	                "请选择要结算的商品！",
	                "提示",
	                JOptionPane.WARNING_MESSAGE);
	        return;
	    }

	    double total = 0;

	    for (OrderItem item : selectedItems) {
	        total += item.getQuantity() * item.getUnitPrice();
	    }

	    final double finalOriginalTotal = total;
	    final double finalRedPacketUsed =
	            Math.min(redPacketAmount, finalOriginalTotal);
	    final double finalPayAmount =
	            Math.max(0, finalOriginalTotal - finalRedPacketUsed);

	    final String userId = extractUserId(loginUser);

	    if (userId == null
	            || userId.trim().isEmpty()
	            || "未登录".equals(userId)) {

	        JOptionPane.showMessageDialog(
	                this,
	                "用户未登录，无法下单",
	                "错误",
	                JOptionPane.ERROR_MESSAGE);
	        return;
	    }

	    String[] options = {
	            "校园卡支付",
	            "银行卡支付"
	    };

	    int choice = JOptionPane.showOptionDialog(
	            cartDialog,
	            "订单原价："
	                    + String.format("%.2f", finalOriginalTotal)
	                    + " 元\n红包抵扣："
	                    + String.format("%.2f", finalRedPacketUsed)
	                    + " 元\n应付金额："
	                    + String.format("%.2f", finalPayAmount)
	                    + " 元\n\n请选择支付方式",
	            "支付",
	            JOptionPane.DEFAULT_OPTION,
	            JOptionPane.QUESTION_MESSAGE,
	            null,
	            options,
	            options[0]);

	    if (choice == JOptionPane.CLOSED_OPTION) {
	        return;
	    }

	    final String payMethod =
	            (choice == 0) ? "CARD" : "BANK";

	    final String payName =
	            (choice == 0) ? "校园卡" : "银行卡";

	    double balance = getShopBalance(userId, payMethod);

	    if (balance < 0) {

	        JOptionPane.showMessageDialog(
	                cartDialog,
	                "未查询到" + payName
	                        + "余额，无法支付。\n"
	                        + "请确认当前账号已绑定或开通对应账户。",
	                "支付",
	                JOptionPane.ERROR_MESSAGE);
	        return;
	    }

	    if (balance < finalPayAmount) {

	        JOptionPane.showMessageDialog(
	                cartDialog,
	                payName + "余额不足，无法支付。\n"
	                        + "当前余额："
	                        + String.format("%.2f", balance)
	                        + " 元\n应付金额："
	                        + String.format("%.2f", finalPayAmount)
	                        + " 元",
	                "支付",
	                JOptionPane.WARNING_MESSAGE);
	        return;
	    }

	    String balanceText =
	            payName + "余额："
	                    + String.format("%.2f", balance)
	                    + " 元\n订单原价："
	                    + String.format("%.2f", finalOriginalTotal)
	                    + " 元\n红包抵扣："
	                    + String.format("%.2f", finalRedPacketUsed)
	                    + " 元\n应付金额："
	                    + String.format("%.2f", finalPayAmount)
	                    + " 元\n支付后余额："
	                    + String.format("%.2f",
	                            balance - finalPayAmount)
	                    + " 元";

	    int confirm = JOptionPane.showConfirmDialog(
	            cartDialog,
	            balanceText + "\n\n确认支付吗？",
	            "确认支付",
	            JOptionPane.OK_CANCEL_OPTION,
	            JOptionPane.QUESTION_MESSAGE);

	    if (confirm != JOptionPane.OK_OPTION) {
	        return;
	    }

	    if (!"BANK".equals(payMethod)) {

	        submitShopOrder(
	                userId,
	                selectedItems,
	                selectedIndexes,
	                payMethod,
	                finalPayAmount,
	                null,
	                finalOriginalTotal,
	                finalRedPacketUsed);

	        return;
	    }

	    String bankPIN = askShopBankPIN();

	    if (bankPIN == null) {
	        return;
	    }

	    submitShopOrder(
	            userId,
	            selectedItems,
	            selectedIndexes,
	            payMethod,
	            finalPayAmount,
	            bankPIN,
	            finalOriginalTotal,
	            finalRedPacketUsed);
	}

	private double getShopBalance(String userId, String payMethod) {
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		params.put("payMethod", payMethod);
		Message response = SocketClient.send(new Message("GET_SHOP_BALANCE", params));
		if (response != null && response.isSuccess() && response.getData() instanceof Number) {
			return ((Number) response.getData()).doubleValue();
		}
		return -1;
	}

	private JPanel createPINPanel(JPasswordField[] fields, JButton showButton) {

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));

		for (int i = 0; i < 6; i++) {

			JPasswordField field = new JPasswordField(1);

			field.setHorizontalAlignment(JPasswordField.CENTER);
			field.setFont(new Font("SansSerif", Font.BOLD, 18));

			final int index = i;

			field.addKeyListener(new java.awt.event.KeyAdapter() {

				@Override
				public void keyTyped(java.awt.event.KeyEvent e) {

					char c = e.getKeyChar();

					if (!Character.isDigit(c)) {
						e.consume();
						return;
					}

					if (field.getPassword().length > 0) {
						e.consume();
						return;
					}

					if (index < 5) {
						SwingUtilities.invokeLater(() -> fields[index + 1].requestFocusInWindow());
					}
				}

				@Override
				public void keyPressed(java.awt.event.KeyEvent e) {

					if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE && field.getPassword().length == 0
							&& index > 0) {

						fields[index - 1].requestFocusInWindow();
					}
				}
			});

			fields[i] = field;
			panel.add(field);
		}

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

	private String askShopBankPIN() {

		JPasswordField[] pinFields = new JPasswordField[6];

		JButton showButton = new JButton("显示");

		JPanel pinPanel = createPINPanel(pinFields, showButton);

		JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

		content.add(new JLabel("银行卡密码:"));

		content.add(pinPanel);

		int result = JOptionPane.showConfirmDialog(this, content, "请输入银行卡密码", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (result != JOptionPane.OK_OPTION) {
			return null;
		}

		String pin = getShopPIN(pinFields);

		if (!pin.matches("\\d{6}")) {

			JOptionPane.showMessageDialog(this, "请输入完整的6位银行卡密码。", "银行卡支付", JOptionPane.WARNING_MESSAGE);

			return null;
		}

		return pin;
	}

	private String getShopPIN(JPasswordField[] fields) {

		StringBuilder pin = new StringBuilder();

		for (JPasswordField field : fields) {
			pin.append(field.getPassword());
		}

		return pin.toString();
	}

	// ---------- 历史订单 ----------
	private void showOrderHistory() {
		String userId = extractUserId(loginUser);
		if (userId == null || userId.trim().isEmpty() || "未登录".equals(userId)) {
			JOptionPane.showMessageDialog(this, "当前用户信息缺少账号，无法查询历史订单。", "历史订单", JOptionPane.WARNING_MESSAGE);
			return;
		}
		final String finalUserId = userId;

		new Thread(() -> {
			try {
				List<Order> orders = ShopClientSrv.getOrdersByUser(finalUserId);
				SwingUtilities.invokeLater(() -> {
					if (orders == null || orders.isEmpty()) {
						JOptionPane.showMessageDialog(this, "暂无订单", "历史订单", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					showOrderHistoryWindow(orders, finalUserId);
				});
			} catch (Exception e) {
				e.printStackTrace();
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "加载订单失败：" + e.getMessage(), "错误",
						JOptionPane.ERROR_MESSAGE));
			}
		}).start();
	}

	private void showOrderHistoryWindow(List<Order> orders, String userId) {
		JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "我的历史订单");
		dialog.setSize(820, 520);
		dialog.setLocationRelativeTo(this);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.getContentPane().setBackground(COLOR_BG);

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(COLOR_PRIMARY);
		topPanel.setBorder(new EmptyBorder(12, 18, 12, 18));
		JLabel title = new JLabel("我的历史订单");
		title.setFont(new Font("微软雅黑", Font.BOLD, 22));
		title.setForeground(Color.WHITE);
		JLabel summary = new JLabel("账号：" + userId + "    共 " + orders.size() + " 笔购物记录");
		summary.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		summary.setForeground(Color.WHITE);
		topPanel.add(title, BorderLayout.WEST);
		topPanel.add(summary, BorderLayout.EAST);
		dialog.add(topPanel, BorderLayout.NORTH);

		String[] columns = { "序号", "订单号", "下单账号", "下单时间", "实付金额(元)", "订单状态" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		for (int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			model.addRow(new Object[] { i + 1, order.getId(), order.getUserId(), formatOrderTime(order),
					String.format("%.2f", order.getTotalAmount()), formatOrderStatus(order.getStatus()) });
		}

		JTable table = new JTable(model);
		table.setRowHeight(30);
		table.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 13));
		table.getTableHeader().setBackground(COLOR_DARK_BTN);
		table.getTableHeader().setForeground(Color.WHITE);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(50);
		table.getColumnModel().getColumn(1).setPreferredWidth(90);
		table.getColumnModel().getColumn(2).setPreferredWidth(120);
		table.getColumnModel().getColumn(3).setPreferredWidth(170);
		table.getColumnModel().getColumn(4).setPreferredWidth(100);
		table.getColumnModel().getColumn(5).setPreferredWidth(100);
		table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (!isSelected) {
					c.setBackground(row % 2 == 0 ? Color.WHITE : COLOR_TABLE_ODD);
					c.setForeground(Color.BLACK);
				} else {
					c.setBackground(COLOR_PRIMARY);
					c.setForeground(Color.WHITE);
				}
				setHorizontalAlignment(column == 3 ? SwingConstants.LEFT : SwingConstants.CENTER);
				return c;
			}
		});

		JTextArea detailArea = new JTextArea();
		detailArea.setEditable(false);
		detailArea.setLineWrap(true);
		detailArea.setWrapStyleWord(true);
		detailArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		detailArea.setBackground(Color.WHITE);
		detailArea.setBorder(new EmptyBorder(10, 12, 10, 12));

		table.getSelectionModel().addListSelectionListener(e -> {
			if (e.getValueIsAdjusting()) return;
			int viewRow = table.getSelectedRow();
			if (viewRow < 0) return;
			int modelRow = table.convertRowIndexToModel(viewRow);
			Order order = orders.get(modelRow);
			detailArea.setText(buildOrderDetailText(order, modelRow + 1));
		});
		if (!orders.isEmpty()) {
			table.setRowSelectionInterval(0, 0);
			detailArea.setText(buildOrderDetailText(orders.get(0), 1));
		}

		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_PRIMARY, 1),
				"每一次购物记录", TitledBorder.LEFT, TitledBorder.TOP, new Font("微软雅黑", Font.BOLD, 13), COLOR_PRIMARY));
		JScrollPane detailScroll = new JScrollPane(detailArea);
		detailScroll.setPreferredSize(new Dimension(800, 120));
		detailScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_ORANGE, 1),
				"选中订单详情", TitledBorder.LEFT, TitledBorder.TOP, new Font("微软雅黑", Font.BOLD, 13), COLOR_ORANGE));

		JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
		centerPanel.setBackground(COLOR_BG);
		centerPanel.setBorder(new EmptyBorder(0, 12, 0, 12));
		centerPanel.add(tableScroll, BorderLayout.CENTER);
		centerPanel.add(detailScroll, BorderLayout.SOUTH);
		dialog.add(centerPanel, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
		bottomPanel.setBackground(COLOR_BG);
		JButton btnClose = new JButton("关闭");
		styleSmallButton(btnClose, COLOR_DARK_BTN);
		btnClose.setPreferredSize(new Dimension(90, 34));
		btnClose.addActionListener(e -> dialog.dispose());
		bottomPanel.add(btnClose);
		dialog.add(bottomPanel, BorderLayout.SOUTH);

		dialog.setVisible(true);
	}

	private String buildOrderDetailText(Order order, int index) {
		StringBuilder detail = new StringBuilder();
		detail.append("第 ").append(index).append(" 次购物记录").append("\n");
		detail.append("订单号：").append(order.getId()).append("\n");
		detail.append("下单账号：").append(order.getUserId()).append("\n");
		detail.append("下单时间：").append(formatOrderTime(order)).append("\n");
		detail.append("实付金额：").append(String.format("%.2f", order.getTotalAmount())).append(" 元\n");
		detail.append("订单状态：").append(formatOrderStatus(order.getStatus())).append("\n");

		List<OrderItem> items = orderItemMap.get(order.getId());
		if (items == null || items.isEmpty()) {
			detail.append("商品明细：当前服务器查询接口未返回历史商品明细。");
			return detail.toString();
		}

		detail.append("商品明细：").append("\n");
		for (OrderItem item : items) {
			Product product = findProductById(item.getProductId());
			String productName = product == null ? "商品ID " + item.getProductId() : product.getName();
			detail.append("  - ")
					.append(productName)
					.append("  数量：")
					.append(item.getQuantity())
					.append("  单价：")
					.append(String.format("%.2f", item.getUnitPrice()))
					.append(" 元  小计：")
					.append(String.format("%.2f", item.calcSubtotal()))
					.append(" 元")
					.append("\n");
		}
		return detail.toString();
	}

	private String formatOrderTime(Order order) {
		if (order == null || order.getCreateTime() == null) {
			return "未知";
		}
		return sdf.format(order.getCreateTime());
	}

	private String formatOrderStatus(OrderStatus status) {
		if (status == null) {
			return "未知";
		}
		switch (status) {
		case CREATED:
			return "已创建";
		case PAID:
			return "已支付";
		case CANCELLED:
			return "已取消";
		case FINISHED:
			return "已完成";
		default:
			return status.name();
		}
	}

	// ---------- 管理员功能 ----------
	private void showManagerDialog() {
		JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "库存管理");
		dialog.setSize(820, 520);
		dialog.setLocationRelativeTo(this);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.getContentPane().setBackground(COLOR_BG);

		String[] columns = { "ID", "名称", "分类", "价格(元)", "库存", "描述" };
		DefaultTableModel managerModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
		JTable managerTable = new JTable(managerModel);
		managerTable.setRowHeight(28);
		managerTable.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		managerTable.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 14));
		managerTable.getTableHeader().setBackground(COLOR_DARK_BTN);
		managerTable.getTableHeader().setForeground(Color.WHITE);
		managerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		refreshManagerTable(managerModel);

		JScrollPane scrollPane = new JScrollPane(managerTable);
		scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_DARK_BTN, 1), "库存列表",
				TitledBorder.LEFT, TitledBorder.TOP, new Font("微软雅黑", Font.BOLD, 13), COLOR_DARK_BTN));
		dialog.add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
		buttonPanel.setBackground(COLOR_BG);

		JButton btnAdd = new JButton("上架商品");
		JButton btnStock = new JButton("增加库存");
		JButton btnDelete = new JButton("下架商品");
		JButton btnRefresh = new JButton("刷新");
		styleManagerButton(btnAdd, COLOR_SECONDARY);
		styleManagerButton(btnStock, COLOR_PRIMARY);
		styleManagerButton(btnDelete, new Color(231, 76, 60));
		styleManagerButton(btnRefresh, COLOR_DARK_BTN);

		btnAdd.addActionListener(e -> addProductByDialog(dialog, managerModel));
		btnStock.addActionListener(e -> addStockByDialog(dialog, managerTable, managerModel));
		btnDelete.addActionListener(e -> deleteProductByDialog(dialog, managerTable, managerModel));
		btnRefresh.addActionListener(e -> reloadProducts(managerModel));

		buttonPanel.add(btnAdd);
		buttonPanel.add(btnStock);
		buttonPanel.add(btnDelete);
		buttonPanel.add(btnRefresh);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		dialog.setVisible(true);
	}

	private void refreshManagerTable(DefaultTableModel managerModel) {
		managerModel.setRowCount(0);
		for (Product p : allProducts) {
			managerModel.addRow(new Object[] { p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getStock(),
					p.getDescription() });
		}
	}

	private void styleManagerButton(JButton button, Color color) {
		button.setFont(new Font("微软雅黑", Font.BOLD, 13));
		button.setBackground(color);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		button.setPreferredSize(new Dimension(110, 36));
	}

	private void addProductByDialog(Component parent, DefaultTableModel managerModel) {
		JTextField nameField = new JTextField();
		JTextField categoryField = new JTextField();
		JTextField priceField = new JTextField();
		JTextField stockField = new JTextField();
		JTextField descField = new JTextField();

		JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
		form.add(new JLabel("名称："));
		form.add(nameField);
		form.add(new JLabel("分类："));
		form.add(categoryField);
		form.add(new JLabel("价格："));
		form.add(priceField);
		form.add(new JLabel("库存："));
		form.add(stockField);
		form.add(new JLabel("描述："));
		form.add(descField);

		int result = JOptionPane.showConfirmDialog(parent, form, "上架商品", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION)
			return;

		try {
			String name = nameField.getText().trim();
			String category = categoryField.getText().trim();
			double price = Double.parseDouble(priceField.getText().trim());
			int stock = Integer.parseInt(stockField.getText().trim());
			String description = descField.getText().trim();

			if (name.isEmpty() || category.isEmpty() || price < 0 || stock < 0) {
				JOptionPane.showMessageDialog(parent, "请填写有效的商品信息。", "提示", JOptionPane.WARNING_MESSAGE);
				return;
			}

			Product product = new Product();
			product.setName(name);
			product.setCategory(category);
			product.setPrice(price);
			product.setStock(stock);
			product.setDescription(description);

			if (ShopClientSrv.addProduct(product)) {
				JOptionPane.showMessageDialog(parent, "商品上架成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
				reloadProducts(managerModel);
			} else {
				JOptionPane.showMessageDialog(parent, "商品上架失败，请检查服务器或数据库。", "错误", JOptionPane.ERROR_MESSAGE);
			}
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(parent, "价格和库存必须填写数字。", "错误", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addStockByDialog(Component parent, JTable managerTable, DefaultTableModel managerModel) {
		int row = managerTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(parent, "请先选择要补货的商品。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}

		int productId = (int) managerModel.getValueAt(row, 0);
		String name = String.valueOf(managerModel.getValueAt(row, 1));
		String input = JOptionPane.showInputDialog(parent, "请输入「" + name + "」要增加的库存数量：", "增加库存",
				JOptionPane.QUESTION_MESSAGE);
		if (input == null)
			return;

		try {
			int amount = Integer.parseInt(input.trim());
			if (amount <= 0) {
				JOptionPane.showMessageDialog(parent, "增加数量必须大于 0。", "提示", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (ShopClientSrv.addStock(productId, amount)) {
				JOptionPane.showMessageDialog(parent, "库存增加成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
				reloadProducts(managerModel);
			} else {
				JOptionPane.showMessageDialog(parent, "库存增加失败，请检查服务器或数据库。", "错误", JOptionPane.ERROR_MESSAGE);
			}
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(parent, "库存数量必须填写整数。", "错误", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void deleteProductByDialog(Component parent, JTable managerTable, DefaultTableModel managerModel) {
		int row = managerTable.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(parent, "请先选择要下架的商品。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}

		int productId = (int) managerModel.getValueAt(row, 0);
		String name = String.valueOf(managerModel.getValueAt(row, 1));
		int confirm = JOptionPane.showConfirmDialog(parent, "确定下架「" + name + "」吗？", "下架商品",
				JOptionPane.OK_CANCEL_OPTION);
		if (confirm != JOptionPane.OK_OPTION)
			return;

		if (ShopClientSrv.offlineProduct(productId)) {
			JOptionPane.showMessageDialog(parent, "商品下架成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
			reloadProducts(managerModel);
		} else {
			JOptionPane.showMessageDialog(parent, "商品下架失败，可能商品不存在。", "错误", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addStockSelectedProduct(Component parent) {
		Product product = productList.getSelectedValue();
		if (product == null) {
			JOptionPane.showMessageDialog(parent, "请先选择要补货的商品。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}

		String input = JOptionPane.showInputDialog(parent, "请输入「" + product.getName() + "」要增加的库存数量：", "增加库存",
				JOptionPane.QUESTION_MESSAGE);
		if (input == null)
			return;

		try {
			int amount = Integer.parseInt(input.trim());
			if (amount <= 0) {
				JOptionPane.showMessageDialog(parent, "增加数量必须大于 0。", "提示", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (ShopClientSrv.addStock(product.getId(), amount)) {
				JOptionPane.showMessageDialog(parent, "库存增加成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
				reloadProducts(null);
			} else {
				JOptionPane.showMessageDialog(parent, "库存增加失败，请检查服务器或数据库。", "错误", JOptionPane.ERROR_MESSAGE);
			}
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(parent, "库存数量必须填写整数。", "错误", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void deleteSelectedProduct(Component parent) {
		Product product = productList.getSelectedValue();
		if (product == null) {
			JOptionPane.showMessageDialog(parent, "请先选择要下架的商品。", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(parent, "确定下架「" + product.getName() + "」吗？", "下架商品",
				JOptionPane.OK_CANCEL_OPTION);
		if (confirm != JOptionPane.OK_OPTION)
			return;

		if (ShopClientSrv.offlineProduct(product.getId())) {
			JOptionPane.showMessageDialog(parent, "商品下架成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
			reloadProducts(null);
		} else {
			JOptionPane.showMessageDialog(parent, "商品下架失败，可能商品不存在。", "错误", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void reloadProducts(DefaultTableModel managerModel) {
		loadProductsFromServer();
		filterProducts((String) categoryCombo.getSelectedItem());
		refreshProductList();
		if (managerModel != null) {
			refreshManagerTable(managerModel);
		}
	}

	private void submitShopOrder(
	        final String userId,
	        final List<OrderItem> selectedItems,
	        final List<Integer> selectedIndexes,
	        final String payMethod,
	        final double finalPayAmount,
	        final String bankPIN,
	        final double finalOriginalTotal,
	        final double finalRedPacketUsed) {

	    new Thread(() -> {

	        try {

	            Map<String, Object> params =
	                    new HashMap<>();

	            params.put("userId", userId);
	            params.put("items", selectedItems);
	            params.put("payMethod", payMethod);
	            params.put("totalAmount", finalPayAmount);

	            if ("BANK".equals(payMethod)) {
	                params.put("bankPIN", bankPIN);
	            }

	            Message paymentResponse =
	                    ShopClientSrv.submitOrderWithPaymentMessage(params);

	            SwingUtilities.invokeLater(() -> {

	                if (paymentResponse == null) {

	                    JOptionPane.showMessageDialog(
	                            this,
	                            "❌ 下单失败，服务器无响应。",
	                            "失败",
	                            JOptionPane.ERROR_MESSAGE);

	                    return;
	                }

	                if (!paymentResponse.isSuccess()
	                        && "银行卡密码错误，请重新输入。"
	                                .equals(paymentResponse.getResponseMsg())) {

	                    JOptionPane.showMessageDialog(
	                            this,
	                            "银行卡密码错误，请重新输入。",
	                            "密码错误",
	                            JOptionPane.WARNING_MESSAGE);

	                    String newBankPIN =
	                            askShopBankPIN();

	                    if (newBankPIN == null) {
	                        return;
	                    }

	                    submitShopOrder(
	                            userId,
	                            selectedItems,
	                            selectedIndexes,
	                            payMethod,
	                            finalPayAmount,
	                            newBankPIN,
	                            finalOriginalTotal,
	                            finalRedPacketUsed);

	                    return;
	                }
	                
	                if (!paymentResponse.isSuccess()) {

	                    JOptionPane.showMessageDialog(
	                            this,
	                            "❌ "
	                                    + paymentResponse.getResponseMsg(),
	                            "失败",
	                            JOptionPane.ERROR_MESSAGE);

	                    return;
	                }

	                Order order =
	                        (Order) paymentResponse.getData();

	                if (order == null) {

	                    JOptionPane.showMessageDialog(
	                            this,
	                            "❌ 下单失败，服务器未返回订单。",
	                            "失败",
	                            JOptionPane.ERROR_MESSAGE);

	                    return;
	                }

	                JOptionPane.showMessageDialog(
	                        this,
	                        "✅ 下单成功！订单号："
	                                + order.getId(),
	                        "成功",
	                        JOptionPane.INFORMATION_MESSAGE);

	                List<OrderItem> historyItems =
	                        new ArrayList<>();
	                for (OrderItem item : selectedItems) {
	                    item.setOrderId(order.getId());
	                    historyItems.add(item);
	                }
	                orderItemMap.put(order.getId(), historyItems);

	                selectedIndexes.sort(
	                        (a, b) -> b - a);

	                for (int index : selectedIndexes) {

	                    if (index >= 0
	                            && index < cartItems.size()) {

	                        cartItems.remove(index);
	                    }
	                }

	                redPacketAmount =
	                        Math.max(
	                                0,
	                                redPacketAmount
	                                        - finalRedPacketUsed);

	                applyPurchasedStockToLocalProducts(
	                        selectedItems);

	                syncCartWithCurrentStock();

	                refreshCartDialog();

	                refreshProductsSilently();

	                if (cartDialog != null) {
	                    cartDialog.setVisible(false);
	                }

	                showOrderHistory();
	            });

	        } catch (Exception e) {

	            e.printStackTrace();

	            SwingUtilities.invokeLater(() ->
	                    JOptionPane.showMessageDialog(
	                            this,
	                            "网络异常：" + e.getMessage(),
	                            "错误",
	                            JOptionPane.ERROR_MESSAGE));
	        }

	    }).start();
	}
	
	// ---------- 工具方法 ----------
	private Product findProductById(int id) {
		for (Product p : allProducts) {
			if (p.getId() == id)
				return p;
		}
		return null;
	}

	// ---------- 主方法（测试） ----------
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("校园商店");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(900, 650);
			frame.setLocationRelativeTo(null);
			frame.setContentPane(new ShopPanel(null));
			frame.setVisible(true);
		});
	}
}
